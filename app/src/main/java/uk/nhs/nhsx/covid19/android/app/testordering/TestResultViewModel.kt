package uk.nhs.nhsx.covid19.android.app.testordering

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
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransitionButStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.TransitionAndStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.hasConfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.FINISH
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ORDER_TEST
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.SHARE_KEYS
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation
import javax.inject.Inject

class TestResultViewModel @Inject constructor(
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val relevantTestResultProvider: RelevantTestResultProvider,
    private val testResultIsolationHandler: TestResultIsolationHandler,
    private val stateMachine: IsolationStateMachine,
    private val submitEmptyData: SubmitEmptyData,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows
) : BaseTestResultViewModel() {
    private var wasAcknowledged = false
    private var preventKeySubmission = false

    private lateinit var testResult: ReceivedTestResult

    override fun onCreate() {
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

            val transition = testResultIsolationHandler.computeTransitionWithTestResult(state, testResult)
            val newStateWithTestResult = when (transition) {
                is TransitionAndStoreTestResult -> transition.newState
                is DoNotTransitionButStoreTestResult -> state
                is TransitionDueToTestResult.Ignore -> state
            }
            preventKeySubmission = transition is TransitionDueToTestResult.Ignore && transition.preventKeySubmission

            val mainState = when (testResult.testResult) {
                POSITIVE -> mainStateWhenPositive(state, newStateWithTestResult)
                NEGATIVE -> mainStateWhenNegative(state, newStateWithTestResult)
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
        state: State,
        newStateWithTestResult: State
    ): TestResultViewState {
        return if (newStateWithTestResult is Isolation) {
            if (testResult.requiresConfirmatoryTest) {
                val isIsolatingDueToPositiveConfirmed =
                    state is Isolation && state.hasConfirmedPositiveTestResult(relevantTestResultProvider)
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

    private fun mainStateWhenNegative(
        state: State,
        newStateWithTestResult: State
    ): TestResultViewState =
        when (state) {
            is Isolation ->
                if (newStateWithTestResult is Isolation)
                    if (newStateWithTestResult.isIndexCase())
                        NegativeAfterPositiveOrSymptomaticWillBeInIsolation // D
                    else
                        NegativeWillBeInIsolation
                else
                    NegativeWontBeInIsolation // A
            is Default -> NegativeNotInIsolation // E
        }

    private fun isKeySubmissionSupported(): Boolean =
        testResult.diagnosisKeySubmissionSupported && !preventKeySubmission

    override fun onActionButtonClicked() {
        when (val buttonAction = viewState.value?.mainState?.buttonAction) {
            FINISH -> {
                acknowledgeTestResult()
                navigationEventLiveData.postValue(NavigationEvent.Finish)
            }

            SHARE_KEYS -> {
                if (isKeySubmissionSupported()) {
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

    override fun acknowledgeTestResultIfNecessary() {
        // We do not acknowledge test results that require key submission here since we want to postpone that until:
        //   - The exposure keys are successfully shared, or
        //   - The user explicitly denies permission to share the exposure keys
        val buttonAction = viewState.value?.mainState?.buttonAction
        if (buttonAction == null ||
            (buttonAction == SHARE_KEYS && isKeySubmissionSupported())
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
}
