package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.CreateIsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
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
    private val createIsolationLogicalState: CreateIsolationLogicalState,
    private val clock: Clock,
) {
    operator fun invoke(): ViewState =
        when (val highestPriorityTestResult = getHighestPriorityTestResult()) {
            is FoundTestResult -> computeViewState(highestPriorityTestResult.testResult)
            NoTestResult -> ViewState(Ignore, stateMachine.remainingDaysInIsolation().toInt())
        }

    private fun computeViewState(testResult: ReceivedTestResult): ViewState {
        val currentState = stateMachine.readState()
        val newState = computeIsolationLogicalStateAfterTestResult(currentState, testResult)

        val currentLogicalState = createIsolationLogicalState(currentState)
        val newLogicalState = createIsolationLogicalState(newState)

        val testResultViewState = when (testResult.testResult) {
            POSITIVE -> computeTestResultViewStateOnPositive(currentLogicalState, newLogicalState, testResult)
            NEGATIVE -> computeTestResultViewStateOnNegative(currentLogicalState, newLogicalState)
            VOID -> computeTestResultViewStateOnVoid(currentLogicalState)
            PLOD -> PlodWillContinueWithCurrentState
        }
        val remainingDaysInIsolation = stateMachine.remainingDaysInIsolation(newLogicalState).toInt()

        return ViewState(testResultViewState, remainingDaysInIsolation)
    }

    private fun computeIsolationLogicalStateAfterTestResult(
        currentState: IsolationState,
        testResult: ReceivedTestResult
    ): IsolationState {
        val transition = testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
            currentState,
            testResult,
            testAcknowledgedDate = Instant.now(clock)
        )
        return when (transition) {
            is Transition -> transition.newState
            is DoNotTransition -> currentState
        }
    }
}
