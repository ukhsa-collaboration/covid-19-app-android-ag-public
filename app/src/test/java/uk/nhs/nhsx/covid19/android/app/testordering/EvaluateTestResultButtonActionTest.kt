package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ShareKeys
import kotlin.test.assertEquals

class EvaluateTestResultButtonActionTest {

    private val isKeySubmissionSupported = mockk<IsKeySubmissionSupported>()
    private val canBookFollowUpTest = mockk<CanBookFollowUpTest>()
    private val currentState = mockk<IsolationLogicalState>()
    private val newState = mockk<IsolationLogicalState>()
    private val testResult = mockk<ReceivedTestResult>()

    private val evaluateTestResultButtonAction =
        EvaluateTestResultButtonAction(isKeySubmissionSupported, canBookFollowUpTest)

    @Test
    fun `when key submission is not supported return Finish`() {
        every { isKeySubmissionSupported(testResult) } returns false

        assertEquals(Finish, evaluateTestResultButtonAction(currentState, newState, testResult))
    }

    @Test
    fun `when key submission is supported and user can book a follow-up test then return ShareKeys with appropriate flag`() {
        every { isKeySubmissionSupported(testResult) } returns true
        every { canBookFollowUpTest(currentState, newState, testResult) } returns true

        assertEquals(
            ShareKeys(bookFollowUpTest = true),
            evaluateTestResultButtonAction(currentState, newState, testResult)
        )
    }

    @Test
    fun `when key submission is supported and user can not book a follow-up test then return ShareKeys with appropriate flag`() {
        every { isKeySubmissionSupported(testResult) } returns true
        every { canBookFollowUpTest(currentState, newState, testResult) } returns false

        assertEquals(
            ShareKeys(bookFollowUpTest = false),
            evaluateTestResultButtonAction(currentState, newState, testResult)
        )
    }
}
