@file:Suppress("DEPRECATION")

package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult4_9
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.OldTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MigrateTestResultsTest {

    private val latestResultsProvider = mockk<LatestTestResultProvider>(relaxed = true)
    private val testResultsProvider = mockk<TestResultsProvider>(relaxed = true)
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxed = true)

    private val migrateTestResult = MigrateTestResults(
        latestResultsProvider,
        testResultsProvider,
        unacknowledgedTestResultsProvider,
        relevantTestResultProvider
    )

    @Test
    fun `migration from LatestTestResultProvider`() {
        val latestTestResult = LatestTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE
        )
        every { latestResultsProvider.latestTestResult } returns latestTestResult

        every { testResultsProvider.testResults.values } returns emptyList()

        migrateTestResult()

        verify { latestResultsProvider.latestTestResult = null }
        verify {
            unacknowledgedTestResultsProvider.add(
                ReceivedTestResult(
                    latestTestResult.diagnosisKeySubmissionToken,
                    latestTestResult.testEndDate,
                    latestTestResult.testResult,
                    testKitType = null,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
        }
    }

    @Test
    fun `migration from TestResultProvider`() {
        every { latestResultsProvider.latestTestResult } returns null

        every { testResultsProvider.testResults.values } returns listOf(
            UNACKNOWLEDGED_POSITIVE_TEST_RESULT,
            UNACKNOWLEDGED_NEGATIVE_TEST_RESULT,
            UNACKNOWLEDGED_VOID_TEST_RESULT,
            ACKNOWLEDGED_POSITIVE_TEST_RESULT,
            ACKNOWLEDGED_NEWER_POSITIVE_TEST_RESULT,
            ACKNOWLEDGED_NEWER_NEGATIVE_TEST_RESULT,
            ACKNOWLEDGED_NEWER_VOID_TEST_RESULT
        )

        migrateTestResult()

        verify {
            unacknowledgedTestResultsProvider.add(
                ReceivedTestResult(
                    UNACKNOWLEDGED_POSITIVE_TEST_RESULT.diagnosisKeySubmissionToken,
                    UNACKNOWLEDGED_POSITIVE_TEST_RESULT.testEndDate,
                    UNACKNOWLEDGED_POSITIVE_TEST_RESULT.testResult,
                    testKitType = null,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
            unacknowledgedTestResultsProvider.add(
                ReceivedTestResult(
                    UNACKNOWLEDGED_POSITIVE_TEST_RESULT.diagnosisKeySubmissionToken,
                    UNACKNOWLEDGED_POSITIVE_TEST_RESULT.testEndDate,
                    UNACKNOWLEDGED_POSITIVE_TEST_RESULT.testResult,
                    testKitType = null,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
            unacknowledgedTestResultsProvider.add(
                ReceivedTestResult(
                    UNACKNOWLEDGED_NEGATIVE_TEST_RESULT.diagnosisKeySubmissionToken,
                    UNACKNOWLEDGED_NEGATIVE_TEST_RESULT.testEndDate,
                    UNACKNOWLEDGED_NEGATIVE_TEST_RESULT.testResult,
                    testKitType = null,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
            unacknowledgedTestResultsProvider.add(
                ReceivedTestResult(
                    UNACKNOWLEDGED_VOID_TEST_RESULT.diagnosisKeySubmissionToken,
                    UNACKNOWLEDGED_VOID_TEST_RESULT.testEndDate,
                    UNACKNOWLEDGED_VOID_TEST_RESULT.testResult,
                    testKitType = null,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )
        }

        verify {
            relevantTestResultProvider.storeMigratedTestResult(
                AcknowledgedTestResult4_9(
                    ACKNOWLEDGED_NEWER_POSITIVE_TEST_RESULT.diagnosisKeySubmissionToken,
                    ACKNOWLEDGED_NEWER_POSITIVE_TEST_RESULT.testEndDate,
                    RelevantVirologyTestResult.POSITIVE,
                    testKitType = null,
                    acknowledgedDate = ACKNOWLEDGED_NEWER_POSITIVE_TEST_RESULT.acknowledgedDate!!,
                    requiresConfirmatoryTest = false,
                    confirmedDate = null
                )
            )
        }

        verify { testResultsProvider.clear() }
    }

    @Test
    fun `no migration needed from LatestTestResultProvider and TestResultProvider`() {
        every { latestResultsProvider.latestTestResult } returns null
        every { testResultsProvider.testResults.values } returns emptyList()

        migrateTestResult()

        verify(exactly = 0) { latestResultsProvider.latestTestResult = any() }
        verify(exactly = 0) { testResultsProvider.clear() }
        verify(exactly = 0) { unacknowledgedTestResultsProvider.add(any()) }
        verify(exactly = 0) { relevantTestResultProvider.storeMigratedTestResult(any()) }
    }

    @Suppress("DEPRECATION")
    companion object {
        private val UNACKNOWLEDGED_POSITIVE_TEST_RESULT = OldTestResult(
            "token1",
            Instant.now(),
            POSITIVE
        )
        private val UNACKNOWLEDGED_NEGATIVE_TEST_RESULT = OldTestResult(
            "token2",
            Instant.now(),
            NEGATIVE
        )
        private val UNACKNOWLEDGED_VOID_TEST_RESULT = OldTestResult(
            "token3",
            Instant.now(),
            VOID
        )
        private val ACKNOWLEDGED_POSITIVE_TEST_RESULT = OldTestResult(
            "token4",
            LocalDate.of(2020, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
            POSITIVE,
            Instant.now()
        )
        private val ACKNOWLEDGED_NEWER_POSITIVE_TEST_RESULT = OldTestResult(
            "token5",
            LocalDate.of(2020, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
            POSITIVE,
            Instant.now()
        )
        private val ACKNOWLEDGED_NEWER_NEGATIVE_TEST_RESULT = OldTestResult(
            "token6",
            LocalDate.of(2020, 3, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
            NEGATIVE,
            Instant.now()
        )
        private val ACKNOWLEDGED_NEWER_VOID_TEST_RESULT = OldTestResult(
            "token7",
            LocalDate.of(2020, 4, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
            VOID,
            Instant.now()
        )
    }
}
