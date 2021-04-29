package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestResultHandlerTest {

    @Suppress("DEPRECATION")
    private val latestResultsProvider = mockk<LatestTestResultProvider>(relaxed = true)

    @Suppress("DEPRECATION")
    private val testResultsProvider = mockk<TestResultsProvider>(relaxed = true)
    private val unacknowledgedTestResultsProvider =
        mockk<UnacknowledgedTestResultsProvider>(relaxed = true)
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val keysSubmissionDateRangeCalculator = mockk<CalculateKeySubmissionDateRange>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private fun createTestResultHandler() = TestResultHandler(
        latestResultsProvider,
        testResultsProvider,
        unacknowledgedTestResultsProvider,
        relevantTestResultProvider,
        keySharingInfoProvider,
        keysSubmissionDateRangeCalculator,
        fixedClock
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

        createTestResultHandler()

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

        createTestResultHandler()

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
                AcknowledgedTestResult(
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

        createTestResultHandler()
        verify(exactly = 0) { latestResultsProvider.latestTestResult = any() }
        verify(exactly = 0) { testResultsProvider.clear() }
        verify(exactly = 0) { unacknowledgedTestResultsProvider.add(any()) }
        verify(exactly = 0) { relevantTestResultProvider.storeMigratedTestResult(any()) }
    }

    @Test
    fun `on positive test result received adds it to unacknowledged test results`() {
        val testSubject = createTestResultHandler()

        testSubject.onTestResultReceived(RECEIVED_POSITIVE_TEST_RESULT)

        verify { unacknowledgedTestResultsProvider.add(RECEIVED_POSITIVE_TEST_RESULT) }
    }

    @Test
    fun `on negative test result received adds it to unacknowledged test results`() {
        val testSubject = createTestResultHandler()

        testSubject.onTestResultReceived(RECEIVED_NEGATIVE_TEST_RESULT)

        verify { unacknowledgedTestResultsProvider.add(RECEIVED_NEGATIVE_TEST_RESULT) }
    }

    @Test
    fun `on void test result received adds it to unacknowledged test results`() {
        val testSubject = createTestResultHandler()

        testSubject.onTestResultReceived(RECEIVED_VOID_TEST_RESULT)

        verify { unacknowledgedTestResultsProvider.add(RECEIVED_VOID_TEST_RESULT) }
    }

    @Test
    fun `on positive test result acknowledge removes it from unacknowledged test results reports it to relevant test result provider, and adds KeySharingInfo`() {
        val dateRangeMock = mockk<SubmissionDateRange>()
        every { dateRangeMock.containsAtLeastOneDay() } returns true
        every { keysSubmissionDateRangeCalculator.invoke(any(), any()) } returns dateRangeMock

        val testSubject = createTestResultHandler()

        testSubject.acknowledge(RECEIVED_POSITIVE_TEST_RESULT, LocalDate.now(fixedClock), testResultStorageOperation = Ignore)

        verify {
            keySharingInfoProvider.keySharingInfo = KeySharingInfo(
                diagnosisKeySubmissionToken = RECEIVED_POSITIVE_TEST_RESULT.diagnosisKeySubmissionToken!!,
                acknowledgedDate = fixedClock.instant(),
                notificationSentDate = null,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false
            )
        }
        verify { unacknowledgedTestResultsProvider.remove(RECEIVED_POSITIVE_TEST_RESULT) }
        verify {
            relevantTestResultProvider.onTestResultAcknowledged(
                RECEIVED_POSITIVE_TEST_RESULT,
                testResultStorageOperation = Ignore
            )
        }
    }

    @Test
    fun `on positive test result acknowledge removes it from unacknowledged test results reports it to relevant test result provider but does not add KeySharingInfo because dateRange is empty`() {
        val dateRangeMock = mockk<SubmissionDateRange>()
        every { dateRangeMock.containsAtLeastOneDay() } returns false
        every { keysSubmissionDateRangeCalculator.invoke(any(), any()) } returns dateRangeMock

        val testSubject = createTestResultHandler()

        testSubject.acknowledge(RECEIVED_POSITIVE_TEST_RESULT, LocalDate.now(fixedClock), testResultStorageOperation = Ignore)

        verify(exactly = 0) { keySharingInfoProvider.keySharingInfo = any() }
        verify { unacknowledgedTestResultsProvider.remove(RECEIVED_POSITIVE_TEST_RESULT) }
        verify {
            relevantTestResultProvider.onTestResultAcknowledged(
                RECEIVED_POSITIVE_TEST_RESULT,
                testResultStorageOperation = Ignore
            )
        }
    }

    @Test
    fun `on negative test result acknowledge removes it from unacknowledged test results and reports it to relevant test result provider`() {
        val testSubject = createTestResultHandler()

        testSubject.acknowledge(RECEIVED_NEGATIVE_TEST_RESULT, LocalDate.now(fixedClock), testResultStorageOperation = Ignore)

        verify(exactly = 0) { keySharingInfoProvider.keySharingInfo = any() }
        verify { unacknowledgedTestResultsProvider.remove(RECEIVED_NEGATIVE_TEST_RESULT) }
        verify {
            relevantTestResultProvider.onTestResultAcknowledged(
                RECEIVED_NEGATIVE_TEST_RESULT,
                testResultStorageOperation = Ignore
            )
        }
    }

    @Test
    fun `on void test result acknowledge removes it from unacknowledged test results and reports it to relevant test result provider`() {
        val testSubject = createTestResultHandler()

        testSubject.acknowledge(RECEIVED_VOID_TEST_RESULT, LocalDate.now(fixedClock), testResultStorageOperation = Overwrite)

        verify(exactly = 0) { keySharingInfoProvider.keySharingInfo = any() }
        verify { unacknowledgedTestResultsProvider.remove(RECEIVED_VOID_TEST_RESULT) }
        verify {
            relevantTestResultProvider.onTestResultAcknowledged(
                RECEIVED_VOID_TEST_RESULT,
                testResultStorageOperation = Overwrite
            )
        }
    }

    @Test
    fun `hasTestResultMatching returns true if there is an unacknowledged test result matching the predicate`() {
        every { unacknowledgedTestResultsProvider.hasTestResultMatching(any()) } returns true
        every { relevantTestResultProvider.hasTestResultMatching(any()) } returns false

        val testSubject = createTestResultHandler()

        val predicate = mockk<(TestResult) -> Boolean>(relaxed = true)
        val result = testSubject.hasTestResultMatching(predicate)

        assertTrue(result)
    }

    @Test
    fun `hasTestResultMatching returns true if there is an acknowledged test result matching the predicate`() {
        every { unacknowledgedTestResultsProvider.hasTestResultMatching(any()) } returns false
        every { relevantTestResultProvider.hasTestResultMatching(any()) } returns true

        val testSubject = createTestResultHandler()

        val predicate = mockk<(TestResult) -> Boolean>(relaxed = true)
        val result = testSubject.hasTestResultMatching(predicate)

        assertTrue(result)
    }

    @Test
    fun `hasTestResultMatching returns true if there are both an unacknowledged and an acknowledged test result matching the predicate`() {
        every { unacknowledgedTestResultsProvider.hasTestResultMatching(any()) } returns true
        every { relevantTestResultProvider.hasTestResultMatching(any()) } returns true

        val testSubject = createTestResultHandler()

        val predicate = mockk<(TestResult) -> Boolean>(relaxed = true)
        val result = testSubject.hasTestResultMatching(predicate)

        assertTrue(result)
    }

    @Test
    fun `hasTestResultMatching returns false if there are neither unacknowledged nor acknowledged test results matching the predicate`() {
        every { unacknowledgedTestResultsProvider.hasTestResultMatching(any()) } returns false
        every { relevantTestResultProvider.hasTestResultMatching(any()) } returns false

        val testSubject = createTestResultHandler()

        val predicate = mockk<(TestResult) -> Boolean>(relaxed = true)
        val result = testSubject.hasTestResultMatching(predicate)

        assertFalse(result)
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

        private val RECEIVED_POSITIVE_TEST_RESULT = ReceivedTestResult(
            "token1",
            Instant.now(),
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        private val RECEIVED_NEGATIVE_TEST_RESULT = ReceivedTestResult(
            "token2",
            Instant.now(),
            NEGATIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        private val RECEIVED_VOID_TEST_RESULT = ReceivedTestResult(
            "token3",
            Instant.now(),
            VOID,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
    }
}
