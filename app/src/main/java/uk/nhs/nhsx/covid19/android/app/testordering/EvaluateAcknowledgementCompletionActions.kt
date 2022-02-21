package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.FollowUpTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.NoTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.RegularTest
import javax.inject.Inject

class EvaluateAcknowledgementCompletionActions @Inject constructor(
    private val isKeySubmissionSupported: IsKeySubmissionSupported,
    private val canBookFollowUpTest: CanBookFollowUpTest
) {
    operator fun invoke(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState,
        testResult: ReceivedTestResult
    ): AcknowledgementCompletionActions {
        val suggestBookTest = when {
            testResult.testResult == VOID -> RegularTest
            canBookFollowUpTest(currentState, newState, testResult) -> FollowUpTest
            else -> NoTest
        }
        return AcknowledgementCompletionActions(
            suggestBookTest = suggestBookTest,
            shouldAllowKeySubmission = isKeySubmissionSupported(testResult)
        )
    }
}
