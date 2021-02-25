package uk.nhs.nhsx.covid19.android.app.testordering

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType

@Singleton
class TestResultHandler @Inject constructor(
    @Suppress("DEPRECATION") private val latestTestResultProvider: LatestTestResultProvider,
    @Suppress("DEPRECATION") private val testResultsProvider: TestResultsProvider,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val relevantTestResultProvider: RelevantTestResultProvider
) : TestResultChecker {

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
                AcknowledgedTestResult(
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
                compareByDescending<AcknowledgedTestResult> { it.testResult.relevance }
                    .thenByDescending { it.testEndDate }
            )
            .firstOrNull()
        relevantTestResultProvider.storeMigratedTestResult(mostRelevantTestResult)

        testResultsProvider.clear()
    }

    fun onTestResultReceived(testResult: ReceivedTestResult) {
        unacknowledgedTestResultsProvider.add(testResult)
    }

    fun acknowledge(testResult: ReceivedTestResult, testResultStorageOperation: TestResultStorageOperation) {
        unacknowledgedTestResultsProvider.remove(testResult)
        relevantTestResultProvider.onTestResultAcknowledged(testResult, testResultStorageOperation)
    }

    override fun hasTestResultMatching(predicate: (TestResult) -> Boolean): Boolean =
        relevantTestResultProvider.hasTestResultMatching(predicate) ||
            unacknowledgedTestResultsProvider.hasTestResultMatching(predicate)
}

interface TestResult {
    val diagnosisKeySubmissionToken: String?
    val testEndDate: Instant
    val testKitType: VirologyTestKitType?
    val requiresConfirmatoryTest: Boolean

    fun isPositive(): Boolean
    fun isConfirmed(): Boolean
}

sealed class TestResultStorageOperation {
    object Overwrite : TestResultStorageOperation()
    data class Confirm(val confirmedDate: Instant) : TestResultStorageOperation()
    data class OverwriteAndConfirm(val confirmedDate: Instant) : TestResultStorageOperation()
    object Ignore : TestResultStorageOperation()
}
