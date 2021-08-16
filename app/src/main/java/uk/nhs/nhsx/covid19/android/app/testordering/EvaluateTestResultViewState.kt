package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.FoundTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.NoTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PlodWillContinueWithCurrentState
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class EvaluateTestResultViewState @Inject constructor(
    private val getHighestPriorityTestResult: GetHighestPriorityTestResult,
    private val stateMachine: IsolationStateMachine,
    private val testResultIsolationHandler: TestResultIsolationHandler,
    private val computeTestResultViewStateOnPositive: ComputeTestResultViewStateOnPositive,
    private val computeTestResultViewStateOnNegative: ComputeTestResultViewStateOnNegative,
    private val computeTestResultViewStateOnVoid: ComputeTestResultViewStateOnVoid,
    private val clock: Clock,
) {
    operator fun invoke(): ViewState =
        when (val highestPriorityTestResult = getHighestPriorityTestResult()) {
            is FoundTestResult -> computeViewState(highestPriorityTestResult.testResult)
            NoTestResult -> ViewState(Ignore, stateMachine.remainingDaysInIsolation().toInt())
        }

    private fun computeViewState(testResult: ReceivedTestResult): ViewState {
        val currentState = stateMachine.readLogicalState()
        val newState = computeIsolationLogicalStateAfterTestResult(currentState, testResult)

        val testResultViewState = when (testResult.testResult) {
            POSITIVE -> computeTestResultViewStateOnPositive(currentState, newState, testResult)
            NEGATIVE -> computeTestResultViewStateOnNegative(currentState, newState)
            VOID -> computeTestResultViewStateOnVoid(currentState)
            PLOD -> PlodWillContinueWithCurrentState
        }
        val remainingDaysInIsolation = stateMachine.remainingDaysInIsolation(newState).toInt()

        return ViewState(testResultViewState, remainingDaysInIsolation)
    }

    private fun computeIsolationLogicalStateAfterTestResult(
        currentState: IsolationLogicalState,
        testResult: ReceivedTestResult
    ): IsolationLogicalState {
        val transition = testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
            currentState,
            testResult,
            testAcknowledgedDate = Instant.now(clock)
        )
        return when (transition) {
            is Transition -> IsolationLogicalState.from(transition.newState)
            is DoNotTransition -> currentState
        }
    }
}
