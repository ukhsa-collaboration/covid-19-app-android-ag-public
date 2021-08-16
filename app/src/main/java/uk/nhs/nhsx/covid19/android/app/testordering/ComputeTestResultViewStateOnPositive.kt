package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import java.time.Clock
import javax.inject.Inject

class ComputeTestResultViewStateOnPositive @Inject constructor(
    private val isKeySubmissionSupported: IsKeySubmissionSupported,
    private val evaluateTestResultButtonAction: EvaluateTestResultButtonAction,
    private val clock: Clock
) {
    operator fun invoke(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState,
        testResult: ReceivedTestResult
    ): TestResultViewState =
        if (newState.isActiveIsolation(clock)) {
            if (testResult.requiresConfirmatoryTest && !isKeySubmissionSupported(testResult)) {
                when {
                    currentState.hasActiveConfirmedPositiveTestResult(clock) -> PositiveContinueIsolationNoChange
                    newState.hasCompletedPositiveTestResult() ->
                        mainStateWhenPositiveNewStateIsIsolating(currentState, newState, testResult)
                    else -> PositiveWillBeInIsolationAndOrderTest
                }
            } else mainStateWhenPositiveNewStateIsIsolating(currentState, newState, testResult)
        } else {
            PositiveWontBeInIsolation(evaluateTestResultButtonAction(currentState, newState, testResult))
        }

    private fun mainStateWhenPositiveNewStateIsIsolating(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState,
        testResult: ReceivedTestResult
    ): TestResultViewState {
        val buttonAction = evaluateTestResultButtonAction(currentState, newState, testResult)

        return if (currentState.isActiveIsolation(clock)) {
            PositiveContinueIsolation(buttonAction)
        } else PositiveWillBeInIsolation(buttonAction)
    }
}
