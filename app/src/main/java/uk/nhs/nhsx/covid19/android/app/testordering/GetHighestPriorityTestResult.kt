package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import javax.inject.Inject

class GetHighestPriorityTestResult @Inject constructor(
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider
) {
    operator fun invoke(): ReceivedTestResult? =
        unacknowledgedTestResultsProvider.testResults
            .minByOrNull { TEST_PRIORITY.indexOf(it.testResult) }

    companion object {
        private val TEST_PRIORITY = listOf(POSITIVE, PLOD, NEGATIVE, VOID)
    }
}
