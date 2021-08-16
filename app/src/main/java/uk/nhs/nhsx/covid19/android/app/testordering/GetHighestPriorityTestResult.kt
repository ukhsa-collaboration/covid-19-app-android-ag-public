package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.FoundTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.NoTestResult
import javax.inject.Inject

class GetHighestPriorityTestResult @Inject constructor(
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider
) {
    operator fun invoke(): HighestPriorityTestResult {
        val highestPriorityTestResult = unacknowledgedTestResultsProvider.testResults
            .minByOrNull { TEST_PRIORITY.indexOf(it.testResult) }
        return if (highestPriorityTestResult == null) {
            NoTestResult
        } else {
            FoundTestResult(highestPriorityTestResult)
        }
    }

    sealed class HighestPriorityTestResult {
        object NoTestResult : HighestPriorityTestResult()
        data class FoundTestResult(val testResult: ReceivedTestResult) : HighestPriorityTestResult()
    }

    companion object {
        private val TEST_PRIORITY = listOf(POSITIVE, PLOD, NEGATIVE, VOID)
    }
}
