package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.FollowUpTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.NoTest
import kotlin.test.assertEquals

class EvaluateAcknowledgementCompletionActionsTest {

    private val isKeySubmissionSupported = mockk<IsKeySubmissionSupported>()
    private val canBookFollowUpTest = mockk<CanBookFollowUpTest>()
    private val currentState = mockk<IsolationLogicalState>()
    private val newState = mockk<IsolationLogicalState>()
    private val testResult = mockk<ReceivedTestResult>()

    private val evaluateTestResultButtonAction =
        EvaluateAcknowledgementCompletionActions(isKeySubmissionSupported, canBookFollowUpTest)

    @Before
    fun setUp() {
        every { testResult.testResult } returns POSITIVE
    }

    @Test
    fun `when key submission is not supported return Finish`() {
        every { isKeySubmissionSupported(testResult) } returns false
        every { canBookFollowUpTest(currentState, newState, testResult) } returns false

        assertEquals(noTestNoKeySubmission, evaluateTestResultButtonAction(currentState, newState, testResult))
    }

    @Test
    fun `when key submission is supported and user can book a follow-up test then return ShareKeys with appropriate flag`() {
        every { isKeySubmissionSupported(testResult) } returns true
        every { canBookFollowUpTest(currentState, newState, testResult) } returns true

        assertEquals(
            shareKeys(bookFollowUpTest = true),
            evaluateTestResultButtonAction(currentState, newState, testResult)
        )
    }

    @Test
    fun `when key submission is supported and user can not book a follow-up test then return ShareKeys with appropriate flag`() {
        every { isKeySubmissionSupported(testResult) } returns true
        every { canBookFollowUpTest(currentState, newState, testResult) } returns false

        assertEquals(
            shareKeys(bookFollowUpTest = false),
            evaluateTestResultButtonAction(currentState, newState, testResult)
        )
    }

    private fun shareKeys(bookFollowUpTest: Boolean): AcknowledgementCompletionActions {
        return AcknowledgementCompletionActions(
            suggestBookTest = if (bookFollowUpTest) FollowUpTest else NoTest,
            shouldAllowKeySubmission = true
        )
    }

    companion object {
        val noTestNoKeySubmission =
            AcknowledgementCompletionActions(suggestBookTest = NoTest, shouldAllowKeySubmission = false)
    }
}
