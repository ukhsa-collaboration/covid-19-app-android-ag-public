package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ShareKeys
import javax.inject.Inject

class EvaluateTestResultButtonAction @Inject constructor(
    private val isKeySubmissionSupported: IsKeySubmissionSupported,
    private val canBookFollowUpTest: CanBookFollowUpTest
) {
    operator fun invoke(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState,
        testResult: ReceivedTestResult
    ): ButtonAction =
        if (isKeySubmissionSupported(testResult)) {
            ShareKeys(canBookFollowUpTest(currentState, newState, testResult))
        } else Finish
}
