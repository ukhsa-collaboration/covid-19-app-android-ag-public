package uk.nhs.nhsx.covid19.android.app.testordering

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmitObfuscationData
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.FINISH
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ORDER_TEST
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.SHARE_KEYS
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class TestResultViewModel @Inject constructor(
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val testResultIsolationHandler: TestResultIsolationHandler,
    private val stateMachine: IsolationStateMachine,
    private val submitObfuscationData: SubmitObfuscationData,
    private val clock: Clock
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

            val currentState = stateMachine.readLogicalState()

            val transition = testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(clock)
            )
            val newState = when (transition) {
                is Transition -> IsolationLogicalState.from(transition.newState)
                is DoNotTransition -> currentState
            }
            preventKeySubmission = transition is DoNotTransition && transition.preventKeySubmission

            val mainState = when (testResult.testResult) {
                POSITIVE -> mainStateWhenPositive(currentState, newState)
                NEGATIVE -> mainStateWhenNegative(currentState, newState)
                VOID -> when {
                    currentState.isActiveIsolation(clock) -> VoidWillBeInIsolation // B
                    else -> VoidNotInIsolation // F
                }
            }
            val remainingDaysInIsolation =
                stateMachine.remainingDaysInIsolation(newState).toInt()
            viewState.postValue(ViewState(mainState, remainingDaysInIsolation))
        }
    }

    private fun mainStateWhenPositive(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState
    ): TestResultViewState =
        if (newState.isActiveIsolation(clock)) {
            if (testResult.requiresConfirmatoryTest) {
                val isIsolatingDueToPositiveConfirmed = currentState is PossiblyIsolating &&
                    currentState.hasActiveConfirmedPositiveTestResult(clock)
                if (isIsolatingDueToPositiveConfirmed) PositiveContinueIsolationNoChange
                else PositiveWillBeInIsolationAndOrderTest
            } else {
                if (currentState.isActiveIsolation(clock)) PositiveContinueIsolation // C
                else PositiveWillBeInIsolation // H
            }
        } else {
            PositiveWontBeInIsolation // G
        }

    private fun mainStateWhenNegative(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState
    ): TestResultViewState =
        if (currentState.isActiveIsolation(clock)) {
            if (newState.isActiveIsolation(clock))
                if (newState is PossiblyIsolating && newState.isActiveIndexCase(clock))
                    NegativeAfterPositiveOrSymptomaticWillBeInIsolation // D
                else
                    NegativeWillBeInIsolation
            else
                NegativeWontBeInIsolation // A
        } else {
            NegativeNotInIsolation // E
        }

    private fun isKeySubmissionSupported(): Boolean =
        testResult.diagnosisKeySubmissionSupported && !preventKeySubmission

    override fun onActionButtonClicked() {
        acknowledgeTestResult()

        when (val buttonAction = viewState.value?.mainState?.buttonAction) {
            FINISH -> {
                submitObfuscationData()
                navigationEventLiveData.postValue(NavigationEvent.Finish)
            }

            SHARE_KEYS -> {
                if (isKeySubmissionSupported()) {
                    navigationEventLiveData.postValue(NavigationEvent.NavigateToShareKeys)
                } else {
                    submitObfuscationData()
                    navigationEventLiveData.postValue(NavigationEvent.Finish)
                }
            }

            ORDER_TEST -> {
                submitObfuscationData()
                navigationEventLiveData.postValue(NavigationEvent.NavigateToOrderTest)
            }

            else -> {
                Timber.d("Unexpected button action $buttonAction")
            }
        }
    }

    override fun onBackPressed() {
        submitObfuscationData()
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
