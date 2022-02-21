package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import java.time.Clock
import javax.inject.Inject

class ComputeTestResultViewStateOnPositive @Inject constructor(
    private val isKeySubmissionSupported: IsKeySubmissionSupported,
    private val clock: Clock
) {
    operator fun invoke(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState,
        testResult: ReceivedTestResult
    ): TestResultViewState =
        if (newState.isActiveIsolation(clock)) {
            if (testResult.requiresConfirmatoryTest && !isKeySubmissionSupported(testResult) &&
                currentState.hasActiveConfirmedPositiveTestResult(clock)
            )
                PositiveContinueIsolationNoChange
            else if (currentState.isActiveIsolation(clock))
                PositiveContinueIsolation
            else
                PositiveWillBeInIsolation
        } else
            PositiveWontBeInIsolation
}
