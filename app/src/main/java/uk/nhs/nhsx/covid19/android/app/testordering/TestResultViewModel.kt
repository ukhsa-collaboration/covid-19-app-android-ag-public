package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW_AFTER_POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.hasConfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.ButtonAction.FINISH
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.ButtonAction.ORDER_TEST
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.ButtonAction.SHARE_KEYS
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveThenNegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class TestResultViewModel @Inject constructor(
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val relevantTestResultProvider: RelevantTestResultProvider,
    private val testResultIsolationHandler: TestResultIsolationHandler,
    private val stateMachine: IsolationStateMachine,
    private val submitEmptyData: SubmitEmptyData,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private val navigationEventLiveData = SingleLiveEvent<NavigationEvent>()
    fun navigationEvent(): LiveData<NavigationEvent> = navigationEventLiveData

    private var wasAcknowledged = false

    private lateinit var testResult: ReceivedTestResult

    fun onCreate() {
        if (viewState.value != null) {
            return
        }

        val receivedTestResult = getHighestPriorityTestResult()
        if (receivedTestResult == null) {
            val remainingDaysInIsolation = stateMachine.remainingDaysInIsolation().toInt()
            viewState.postValue(ViewState(Ignore, remainingDaysInIsolation))
        } else {
            testResult = receivedTestResult

            val state = stateMachine.readState()
            val newStateWithTestResult =
                testResultIsolationHandler.computeNextStateWithTestResult(state, testResult)
            val willBeInIsolation = newStateWithTestResult is Isolation

            val mainState = when (testResult.testResult) {
                POSITIVE -> mainStateWhenPositive(willBeInIsolation, state)
                NEGATIVE -> when (state) {
                    is Isolation -> when (state.hasConfirmedPositiveTestResult(relevantTestResultProvider)) {
                        true -> PositiveThenNegativeWillBeInIsolation // D
                        false -> {
                            if (willBeInIsolation)
                                NegativeWillBeInIsolation
                            else
                                NegativeWontBeInIsolation // A
                        }
                    }
                    is Default -> NegativeNotInIsolation // E
                }
                VOID -> when (state) {
                    is Isolation -> VoidWillBeInIsolation // B
                    is Default -> VoidNotInIsolation // F
                }
            }
            val remainingDaysInIsolation =
                stateMachine.remainingDaysInIsolation(newStateWithTestResult).toInt()
            viewState.postValue(ViewState(mainState, remainingDaysInIsolation))
        }
    }

    private fun mainStateWhenPositive(
        willBeInIsolation: Boolean,
        state: State
    ): MainState {
        return if (willBeInIsolation) {
            if (testResult.requiresConfirmatoryTest) {
                val isIsolatingDueToPositiveConfirmed = state is Isolation &&
                    state.hasConfirmedPositiveTestResult(relevantTestResultProvider)
                if (isIsolatingDueToPositiveConfirmed) PositiveContinueIsolationNoChange
                else PositiveWillBeInIsolationAndOrderTest
            } else {
                when (state) {
                    is Isolation -> PositiveContinueIsolation // C
                    is Default -> PositiveWillBeInIsolation // H
                }
            }
        } else {
            PositiveWontBeInIsolation // G
        }
    }

    fun onActionButtonClicked() {
        when (val buttonAction = viewState.value?.mainState?.buttonAction) {
            FINISH -> {
                acknowledgeTestResult()
                navigationEventLiveData.postValue(NavigationEvent.Finish)
            }

            SHARE_KEYS -> {
                if (testResult.diagnosisKeySubmissionSupported) {
                    navigationEventLiveData.postValue(NavigationEvent.NavigateToShareKeys(testResult))
                } else {
                    acknowledgeTestResult()
                    navigationEventLiveData.postValue(NavigationEvent.Finish)
                }
            }

            ORDER_TEST -> {
                acknowledgeTestResult()
                navigationEventLiveData.postValue(NavigationEvent.NavigateToOrderTest)
            }

            else -> {
                Timber.d("Unexpected button action $buttonAction")
            }
        }
    }

    fun onBackPressed() {
        acknowledgeTestResultIfNecessary()
    }

    private fun acknowledgeTestResultIfNecessary() {
        // We do not acknowledge test results that require key submission here since we want to postpone that until:
        //   - The exposure keys are successfully shared, or
        //   - The user explicitly denies permission to share the exposure keys
        val buttonAction = viewState.value?.mainState?.buttonAction
        if (buttonAction == null ||
            (buttonAction == SHARE_KEYS && testResult.diagnosisKeySubmissionSupported)
        ) {
            return
        }

        acknowledgeTestResult()
    }

    private fun acknowledgeTestResult() {
        if (wasAcknowledged) {
            return
        }

        wasAcknowledged = true
        stateMachine.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        submitFakeExposureWindows(EXPOSURE_WINDOW_AFTER_POSITIVE)
        submitEmptyData(KEY_SUBMISSION)
    }

    private fun getHighestPriorityTestResult(): ReceivedTestResult? {
        unacknowledgedTestResultsProvider.testResults
            .firstOrNull { it.testResult == POSITIVE }
            ?.let { return it }

        unacknowledgedTestResultsProvider.testResults
            .firstOrNull { it.testResult == NEGATIVE }
            ?.let { return it }

        unacknowledgedTestResultsProvider.testResults
            .firstOrNull { it.testResult == VOID }
            ?.let { return it }

        return null
    }

    data class ViewState(
        val mainState: MainState,
        val remainingDaysInIsolation: Int
    )

    sealed class MainState(val buttonAction: ButtonAction) {
        object NegativeNotInIsolation : MainState(buttonAction = FINISH) // E
        object NegativeWillBeInIsolation : MainState(buttonAction = FINISH) // ?
        object NegativeWontBeInIsolation : MainState(buttonAction = FINISH) // A
        object PositiveWillBeInIsolation : MainState(buttonAction = SHARE_KEYS) // H
        object PositiveContinueIsolation : MainState(buttonAction = SHARE_KEYS) // C
        object PositiveContinueIsolationNoChange : MainState(buttonAction = FINISH)
        object PositiveWontBeInIsolation : MainState(buttonAction = SHARE_KEYS) // G
        object PositiveThenNegativeWillBeInIsolation : MainState(buttonAction = FINISH) // D
        object PositiveWillBeInIsolationAndOrderTest : MainState(buttonAction = ORDER_TEST)
        object VoidNotInIsolation : MainState(buttonAction = ORDER_TEST) // F
        object VoidWillBeInIsolation : MainState(buttonAction = ORDER_TEST) // B
        object Ignore : MainState(buttonAction = FINISH)
    }

    enum class ButtonAction {
        SHARE_KEYS,
        ORDER_TEST,
        FINISH
    }

    sealed class NavigationEvent {
        data class NavigateToShareKeys(val testResult: ReceivedTestResult) : NavigationEvent()
        object NavigateToOrderTest : NavigationEvent()
        object Finish : NavigationEvent()
    }
}
