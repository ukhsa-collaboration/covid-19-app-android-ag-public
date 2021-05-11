@file:Suppress("DEPRECATION")

package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult4_9
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import uk.nhs.nhsx.covid19.android.app.testordering.toRelevantVirologyTestResult
import javax.inject.Inject

@Deprecated("This is not actually deprecated but should not be used anywhere apart from MigrateIsolationState. This annotation is used as a warning")
class MigrateTestResults @Inject constructor(
    private val latestTestResultProvider: LatestTestResultProvider,
    private val testResultsProvider: TestResultsProvider,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val relevantTestResultProvider: RelevantTestResultProvider
) {

    private val lock = Object()

    operator fun invoke() = synchronized(lock) {
        migrateLatestTestResultProvider()
        migrateTestResultsProvider()
    }

    private fun migrateLatestTestResultProvider() {
        latestTestResultProvider.latestTestResult?.let {
            unacknowledgedTestResultsProvider.add(
                ReceivedTestResult(
                    it.diagnosisKeySubmissionToken,
                    it.testEndDate,
                    it.testResult,
                    testKitType = null,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
            latestTestResultProvider.latestTestResult = null
        }
    }

    private fun migrateTestResultsProvider() {
        val testResults = testResultsProvider.testResults.values
        if (testResults.isEmpty()) {
            return
        }

        // Unacknowledged test results
        testResults
            .filter { it.acknowledgedDate == null }
            .forEach { testResult ->
                unacknowledgedTestResultsProvider.add(
                    ReceivedTestResult(
                        testResult.diagnosisKeySubmissionToken,
                        testResult.testEndDate,
                        testResult.testResult,
                        testKitType = null,
                        diagnosisKeySubmissionSupported = true,
                        requiresConfirmatoryTest = false
                    )
                )
            }

        // Acknowledged test results
        val mostRelevantTestResult = testResults
            .filter { it.acknowledgedDate != null && it.testResult.toRelevantVirologyTestResult() != null }
            .map {
                AcknowledgedTestResult4_9(
                    it.diagnosisKeySubmissionToken,
                    it.testEndDate,
                    it.testResult.toRelevantVirologyTestResult()!!,
                    testKitType = null,
                    acknowledgedDate = it.acknowledgedDate!!,
                    requiresConfirmatoryTest = false,
                    confirmedDate = null
                )
            }
            .sortedWith(
                compareByDescending<AcknowledgedTestResult4_9> { it.testResult.relevance }
                    .thenByDescending { it.testEndDate }
            )
            .firstOrNull()
        relevantTestResultProvider.storeMigratedTestResult(mostRelevantTestResult)

        testResultsProvider.clear()
    }
}
