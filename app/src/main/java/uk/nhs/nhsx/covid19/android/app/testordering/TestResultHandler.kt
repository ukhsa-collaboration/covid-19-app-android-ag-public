package uk.nhs.nhsx.covid19.android.app.testordering

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestResultHandler @Inject constructor(
    @Suppress("DEPRECATION") private val latestTestResultProvider: LatestTestResultProvider,
    @Suppress("DEPRECATION") private val testResultsProvider: TestResultsProvider,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val relevantTestResultProvider: RelevantTestResultProvider
) {

    init {
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
                    diagnosisKeySubmissionSupported = true
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
                        diagnosisKeySubmissionSupported = true
                    )
                )
            }

        // Acknowledged test results
        testResults
            .filter { it.acknowledgedDate != null && it.testResult.toRelevantVirologyTestResult() != null }
            .map {
                AcknowledgedTestResult(
                    it.diagnosisKeySubmissionToken,
                    it.testEndDate,
                    it.testResult.toRelevantVirologyTestResult()!!,
                    testKitType = null,
                    it.acknowledgedDate!!
                )
            }
            .forEach { relevantTestResultProvider.updateIfRelevant(it) }

        testResultsProvider.clear()
    }

    fun onTestResultReceived(testResult: ReceivedTestResult) {
        unacknowledgedTestResultsProvider.add(testResult)
    }

    fun acknowledge(testResult: ReceivedTestResult) {
        unacknowledgedTestResultsProvider.remove(testResult)
        relevantTestResultProvider.onTestResultAcknowledged(testResult)
    }

    fun hasPositiveTestResultAfter(instant: Instant): Boolean {
        return relevantTestResultProvider.hasPositiveTestResultAfter(instant) ||
            unacknowledgedTestResultsProvider.hasPositiveTestResultAfter(instant)
    }

    fun hasPositiveTestResultAfterOrEqual(instant: Instant): Boolean {
        return relevantTestResultProvider.hasPositiveTestResultAfterOrEqual(instant) ||
            unacknowledgedTestResultsProvider.hasPositiveTestResultAfterOrEqual(instant)
    }
}
