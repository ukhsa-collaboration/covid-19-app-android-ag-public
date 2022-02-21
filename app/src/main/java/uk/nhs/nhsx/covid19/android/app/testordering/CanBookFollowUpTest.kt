package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import java.time.Clock
import javax.inject.Inject

class CanBookFollowUpTest @Inject constructor(
    private val clock: Clock
) {
    operator fun invoke(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState,
        testResult: ReceivedTestResult
    ): Boolean =
        testResult.requiresConfirmatoryTest && testResult.shouldOfferFollowUpTest != false &&
            !currentState.hasActiveConfirmedPositiveTestResult(clock) &&
            !newState.hasCompletedPositiveTestResult()
}
