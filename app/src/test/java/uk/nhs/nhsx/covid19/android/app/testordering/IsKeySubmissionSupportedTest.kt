package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsKeySubmissionSupportedTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val testResultIsolationHandler = mockk<TestResultIsolationHandler>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val isKeySubmissionSupported =
        IsKeySubmissionSupported(isolationStateMachine, testResultIsolationHandler, fixedClock)

    private val currentIsolationState = mockk<IsolationState>()
    private val testResult = mockk<ReceivedTestResult>()

    @Before
    fun setUp() {
        every { testResult.isPositive() } returns true
    }

    @Test
    fun `when test result is not positive return false`() {
        every { testResult.isPositive() } returns false

        assertFalse(isKeySubmissionSupported(testResult))
    }

    @Test
    fun `when test result does not support key submission then return false`() {
        every { testResult.diagnosisKeySubmissionSupported } returns false

        assertFalse(isKeySubmissionSupported(testResult))
    }

    @Test
    fun `when test result supports key submission but key submission should be prevented then return false`() {
        every { testResult.diagnosisKeySubmissionSupported } returns true
        every { isolationStateMachine.readState() } returns currentIsolationState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentIsolationState,
                testResult,
                Instant.now(fixedClock)
            )
        } returns DoNotTransition(preventKeySubmission = true, keySharingInfo = null)

        assertFalse(isKeySubmissionSupported(testResult))
    }

    @Test
    fun `when test result supports key submission and key submission not prevented then return true`() {
        every { testResult.diagnosisKeySubmissionSupported } returns true
        every { isolationStateMachine.readState() } returns currentIsolationState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentIsolationState,
                testResult,
                Instant.now(fixedClock)
            )
        } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

        assertTrue(isKeySubmissionSupported(testResult))
    }

    @Test
    fun `when test result supports key submission and state transition imminent then return true`() {
        every { testResult.diagnosisKeySubmissionSupported } returns true
        every { isolationStateMachine.readState() } returns currentIsolationState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentIsolationState,
                testResult,
                Instant.now(fixedClock)
            )
        } returns Transition(mockk(), keySharingInfo = null)

        assertTrue(isKeySubmissionSupported(testResult))
    }
}
