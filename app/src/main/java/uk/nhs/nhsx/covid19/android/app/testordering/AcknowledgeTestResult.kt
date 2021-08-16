package uk.nhs.nhsx.covid19.android.app.testordering

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmitEpidemiologyDataForTestResult
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmitObfuscationData
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.FoundTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.NoTestResult
import javax.inject.Inject

class AcknowledgeTestResult @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val submitObfuscationData: SubmitObfuscationData,
    private val submitEmptyData: SubmitEmptyData,
    private val submitEpidemiologyDataForTestResult: SubmitEpidemiologyDataForTestResult,
    private val isKeySubmissionSupported: IsKeySubmissionSupported,
    private val getHighestPriorityTestResult: GetHighestPriorityTestResult
) {
    operator fun invoke() {
        when (val highestPriorityTestResult = getHighestPriorityTestResult()) {
            is FoundTestResult -> acknowledge(highestPriorityTestResult.testResult)
            NoTestResult -> Timber.d("No test result found to acknowledge")
        }
    }

    private fun acknowledge(testResult: ReceivedTestResult) {
        submitEpidemiologyData(testResult)
        isolationStateMachine.processEvent(OnTestResultAcknowledge(testResult))
    }

    private fun submitEpidemiologyData(testResult: ReceivedTestResult) {
        with(testResult) {
            if (isPositive()) {
                submitEpidemiologyDataForTestResult(testKitType, requiresConfirmatoryTest)
                if (!isKeySubmissionSupported(testResult)) {
                    submitEmptyData()
                }
            } else {
                submitObfuscationData()
            }
        }
    }
}
