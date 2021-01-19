package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW_AFTER_POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.newStateWithTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveThenNegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import javax.inject.Inject

class TestResultViewModel @Inject constructor(
    private val testResultsProvider: TestResultsProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val stateMachine: IsolationStateMachine,
    private val submitEmptyData: SubmitEmptyData,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows,
    private val clock: Clock
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private val navigateToShareKeysLiveData = SingleLiveEvent<ReceivedTestResult>()
    fun navigateToShareKeys(): LiveData<ReceivedTestResult> = navigateToShareKeysLiveData

    private var wasAcknowledged = false

    @VisibleForTesting
    internal lateinit var testResult: ReceivedTestResult

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
                state.newStateWithTestResult(
                    testResultsProvider,
                    isolationConfigurationProvider,
                    testResult,
                    clock
                )
            val willBeInIsolation = newStateWithTestResult is Isolation

            val mainState = when (testResult.testResult) {
                POSITIVE -> mainStateWhenPositive(willBeInIsolation, state)
                NEGATIVE -> when (state) {
                    is Isolation -> when (testResultsProvider.isLastRelevantTestResultPositive()) {
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
        return when (state) {
            is Isolation -> mainStateWhenPositiveIgnoringLastTestResult(willBeInIsolation)
            is Default -> {
                if (willBeInIsolation)
                    PositiveWillBeInIsolation(testResult.diagnosisKeySubmissionToken) // H
                else
                    PositiveWontBeInIsolation(testResult.diagnosisKeySubmissionToken) // G
            }
        }
    }

    private fun mainStateWhenPositiveIgnoringLastTestResult(willBeInIsolation: Boolean): MainState {
        return when (willBeInIsolation) {
            true -> PositiveContinueIsolation(testResult.diagnosisKeySubmissionToken) // C
            false -> PositiveWontBeInIsolation(testResult.diagnosisKeySubmissionToken) // G
        }
    }

    fun onActionButtonForPositiveTestResultClicked() {
        navigateToShareKeysLiveData.postValue(testResult)
    }

    fun acknowledgeTestResult() {
        // We do not acknowledge positive test results here since we want to postpone that until:
        //   - The exposure keys are successfully shared, or
        //   - The user explicitly denies permission to share the exposure keys
        val willBeAcknowledgedOnNextScreen = testResult.testResult == POSITIVE
        if (wasAcknowledged || willBeAcknowledgedOnNextScreen) {
            return
        }

        wasAcknowledged = true

        viewState.value?.let {
            when (it.mainState) {
                PositiveThenNegativeWillBeInIsolation ->
                    stateMachine.processEvent(
                        OnTestResultAcknowledge(testResult, removeTestResult = true)
                    )
                else ->
                    stateMachine.processEvent(
                        OnTestResultAcknowledge(testResult, removeTestResult = false)
                    )
            }
        }
        submitFakeExposureWindows(EXPOSURE_WINDOW_AFTER_POSITIVE)
        submitEmptyData(KEY_SUBMISSION)
    }

    private fun getHighestPriorityTestResult(): ReceivedTestResult? {
        testResultsProvider.testResults.values
            .filter { it.acknowledgedDate == null }
            .firstOrNull { it.testResult == POSITIVE }
            ?.let { return it }

        testResultsProvider.testResults.values
            .filter { it.acknowledgedDate == null }
            .firstOrNull { it.testResult == NEGATIVE }
            ?.let { return it }

        testResultsProvider.testResults.values
            .filter { it.acknowledgedDate == null }
            .firstOrNull { it.testResult == VOID }
            ?.let { return it }

        return null
    }

    data class ViewState(
        val mainState: MainState,
        val remainingDaysInIsolation: Int
    )

    sealed class MainState {
        object NegativeNotInIsolation : MainState() // E
        object NegativeWillBeInIsolation : MainState() // ?
        object NegativeWontBeInIsolation : MainState() // A
        data class PositiveWillBeInIsolation(val diagnosisKeySubmissionToken: String) : MainState() // H
        data class PositiveContinueIsolation(val diagnosisKeySubmissionToken: String) : MainState() // C
        data class PositiveWontBeInIsolation(val diagnosisKeySubmissionToken: String) : MainState() // G
        object PositiveThenNegativeWillBeInIsolation : MainState() // D
        object VoidNotInIsolation : MainState() // F
        object VoidWillBeInIsolation : MainState() // B
        object Ignore : MainState()
    }
}
