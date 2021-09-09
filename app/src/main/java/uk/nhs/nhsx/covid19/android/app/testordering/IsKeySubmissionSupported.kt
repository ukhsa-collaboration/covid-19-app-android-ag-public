package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class IsKeySubmissionSupported @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val testResultIsolationHandler: TestResultIsolationHandler,
    private val clock: Clock
) {
    operator fun invoke(testResult: ReceivedTestResult): Boolean =
        testResult.diagnosisKeySubmissionSupported && !shouldPreventKeySubmission(testResult)

    private fun shouldPreventKeySubmission(testResult: ReceivedTestResult): Boolean {
        val currentState = isolationStateMachine.readState()
        val transition = testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
            currentState,
            testResult,
            testAcknowledgedDate = Instant.now(clock)
        )
        return transition is DoNotTransition && transition.preventKeySubmission
    }
}
