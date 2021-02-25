package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Confirm
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.OverwriteAndConfirm
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RelevantTestResultProviderTest {

    private val relevantTestResultStorage = mockk<RelevantTestResultStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()
    private val fixedClock = Clock.fixed(Instant.parse("2020-10-07T00:05:00.00Z"), ZoneOffset.UTC)

    private val testSubject = RelevantTestResultProvider(
        relevantTestResultStorage,
        fixedClock,
        moshi
    )

    @Test
    fun `isTestResultPositive returns true for positive test result`() {
        every { relevantTestResultStorage.value } returns POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON

        assertTrue(testSubject.isTestResultPositive())
    }

    @Test
    fun `isTestResultPositive returns false for negative test result`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_CONFIRMED_TEST_RESULT_JSON

        assertFalse(testSubject.isTestResultPositive())
    }

    @Test
    fun `isTestResultPositive returns false for no test`() {
        every { relevantTestResultStorage.value } returns null

        assertFalse(testSubject.isTestResultPositive())
    }

    @Test
    fun `isTestResultNegative returns true for negative test result`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_CONFIRMED_TEST_RESULT_JSON

        assertTrue(testSubject.isTestResultNegative())
    }

    @Test
    fun `isTestResultNegative returns false for positive test result`() {
        every { relevantTestResultStorage.value } returns POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON

        assertFalse(testSubject.isTestResultNegative())
    }

    @Test
    fun `isTestResultNegative returns false for no test`() {
        every { relevantTestResultStorage.value } returns null

        assertFalse(testSubject.isTestResultNegative())
    }

    @Test
    fun `getTestResultIfPositive returns test if positive`() {
        every { relevantTestResultStorage.value } returns POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON

        assertEquals(POSITIVE_INDICATIVE_TEST_RESULT, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive null if negative`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_CONFIRMED_TEST_RESULT_JSON

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive null if no test`() {
        every { relevantTestResultStorage.value } returns null

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `hasTestResultMatching returns true when test matches predicate`() {
        every { relevantTestResultStorage.value } returns POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON

        val result = testSubject.hasTestResultMatching(TestResult::isPositive)

        assertTrue(result)
    }

    @Test
    fun `hasTestResultMatching returns false when test does not match predicate`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_CONFIRMED_TEST_RESULT_JSON

        val result = testSubject.hasTestResultMatching(TestResult::isPositive)

        assertFalse(result)
    }

    @Test
    fun `hasTestResultMatching returns false when there is no test`() {
        every { relevantTestResultStorage.value } returns null

        val result = testSubject.hasTestResultMatching(TestResult::isPositive)

        assertFalse(result)
    }

    @Test
    fun `onTestResultAcknowledged ignores void test result`() {
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                Instant.parse("1970-01-01T00:00:00Z"),
                VirologyTestResult.VOID,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            ),
            testResultStorageOperation = Overwrite
        )

        verify(exactly = 0) { relevantTestResultStorage setProperty "value" value any<String>() }
    }

    @Test
    fun `onTestResultAcknowledged overwrites test when operation is Overwrite`() {
        every { relevantTestResultStorage.value } returns POSITIVE_CONFIRMED_TEST_RESULT_JSON

        val testEndDate = Instant.parse("1970-01-02T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.POSITIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            ),
            testResultStorageOperation = Overwrite
        )

        val expectedResult =
            """
            {"diagnosisKeySubmissionToken":"newToken","testEndDate":"$testEndDate","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","acknowledgedDate":"${Instant.now(fixedClock)}","requiresConfirmatoryTest":false}
            """.trimIndent()

        verify { relevantTestResultStorage setProperty "value" value eq(expectedResult) }
    }

    @Test
    fun `onTestResultAcknowledged overwrites and confirms test when operation is OverwriteAndConfirm`() {
        every { relevantTestResultStorage.value } returns POSITIVE_CONFIRMED_TEST_RESULT_JSON

        val confirmedDate = Instant.parse("1972-01-02T00:00:00Z")
        val testEndDate = Instant.parse("1970-01-02T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.POSITIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = true
            ),
            testResultStorageOperation = OverwriteAndConfirm(confirmedDate)
        )

        val expectedResult =
            """
            {"diagnosisKeySubmissionToken":"newToken","testEndDate":"$testEndDate","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","acknowledgedDate":"${Instant.now(fixedClock)}","requiresConfirmatoryTest":true,"confirmedDate":"$confirmedDate"}
            """.trimIndent()

        verify { relevantTestResultStorage setProperty "value" value eq(expectedResult) }
    }

    @Test
    fun `onTestResultAcknowledged confirms test result when operation is Confirm`() {
        every { relevantTestResultStorage.value } returns POSITIVE_INDICATIVE_UNCONFIRMED_TEST_RESULT_JSON

        val confirmedDate = Instant.parse("1972-01-02T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                Instant.parse("1970-01-02T00:00:00Z"),
                VirologyTestResult.POSITIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            ),
            testResultStorageOperation = Confirm(confirmedDate)
        )

        val expectedResult =
            """
            {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"RAPID_RESULT","acknowledgedDate":"$ACKNOWLEDGED_DATE","requiresConfirmatoryTest":true,"confirmedDate":"$confirmedDate"}
            """.trimIndent()

        verify { relevantTestResultStorage setProperty "value" value eq(expectedResult) }
    }

    @Test
    fun `onTestResultAcknowledged does not overwrite test result when operation is Ignore`() {
        every { relevantTestResultStorage.value } returns POSITIVE_CONFIRMED_TEST_RESULT_JSON

        val testEndDate = Instant.parse("1970-01-02T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.NEGATIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            ),
            testResultStorageOperation = Ignore
        )

        verify(exactly = 0) { relevantTestResultStorage setProperty "value" value any<String>() }
    }

    @Test
    fun `clear test result`() {
        every { relevantTestResultStorage.value } returns POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON

        testSubject.clear()

        verify { relevantTestResultStorage setProperty "value" value null }
    }

    @Test
    fun `with positive assisted LFD test storage`() {
        every { relevantTestResultStorage.value } returns POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON

        assertEquals(POSITIVE_INDICATIVE_TEST_RESULT, testSubject.testResult)
    }

    @Test
    fun `with positive unassisted LFD test storage`() {
        every { relevantTestResultStorage.value } returns POSITIVE_INDICATIVE_SELF_REPORTED_TEST_RESULT_JSON

        assertEquals(POSITIVE_INDICATIVE_SELF_REPORTED_TEST_RESULT, testSubject.testResult)
    }

    @Test
    fun `with positive test without requires confirmatory test storage`() {
        every { relevantTestResultStorage.value } returns POSITIVE_CONFIRMED_TEST_RESULT_JSON

        assertEquals(POSITIVE_CONFIRMED_TEST_RESULT, testSubject.testResult)
    }

    @Test
    fun `with negative test storage`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_CONFIRMED_TEST_RESULT_JSON

        assertEquals(NEGATIVE_CONFIRMED_TEST_RESULT, testSubject.testResult)
    }

    @Test
    fun `with empty storage`() {
        every { relevantTestResultStorage.value } returns null

        assertNull(testSubject.testResult)
    }

    @Test
    fun `with corrupt storage`() {
        every { relevantTestResultStorage.value } returns "sdsfljghsfgyldfjg"

        assertNull(testSubject.testResult)
    }

    companion object {
        private val TEST_END_DATE: Instant = Instant.parse("1970-01-01T00:00:00Z")
        private val ACKNOWLEDGED_DATE: Instant = Instant.parse("2020-07-26T10:00:00Z")
        private val CONFIRMED_DATE: Instant = Instant.parse("2020-07-30T10:00:00Z")

        val POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON =
            """
           {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"RAPID_RESULT","acknowledgedDate":"$ACKNOWLEDGED_DATE","requiresConfirmatoryTest":true,"confirmedDate":"$CONFIRMED_DATE"}
            """.trimIndent()

        val POSITIVE_INDICATIVE_UNCONFIRMED_TEST_RESULT_JSON =
            """
           {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"RAPID_RESULT","acknowledgedDate":"$ACKNOWLEDGED_DATE","requiresConfirmatoryTest":true}
            """.trimIndent()

        val POSITIVE_INDICATIVE_SELF_REPORTED_TEST_RESULT_JSON =
            """
           {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","acknowledgedDate":"$ACKNOWLEDGED_DATE","requiresConfirmatoryTest":true}
            """.trimIndent()

        val NEGATIVE_CONFIRMED_TEST_RESULT_JSON =
            """
            {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"NEGATIVE","testKitType":"LAB_RESULT","acknowledgedDate":"$ACKNOWLEDGED_DATE","requiresConfirmatoryTest":false}
            """.trimIndent()

        val POSITIVE_CONFIRMED_TEST_RESULT_JSON =
            """
           {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"RAPID_RESULT","acknowledgedDate":"$ACKNOWLEDGED_DATE"}
            """.trimIndent()

        val POSITIVE_INDICATIVE_TEST_RESULT = AcknowledgedTestResult(
            "token",
            TEST_END_DATE,
            POSITIVE,
            RAPID_RESULT,
            ACKNOWLEDGED_DATE,
            requiresConfirmatoryTest = true,
            confirmedDate = CONFIRMED_DATE
        )

        val POSITIVE_INDICATIVE_SELF_REPORTED_TEST_RESULT = AcknowledgedTestResult(
            "token",
            TEST_END_DATE,
            POSITIVE,
            RAPID_SELF_REPORTED,
            ACKNOWLEDGED_DATE,
            requiresConfirmatoryTest = true
        )

        val NEGATIVE_CONFIRMED_TEST_RESULT = AcknowledgedTestResult(
            "token",
            TEST_END_DATE,
            NEGATIVE,
            LAB_RESULT,
            ACKNOWLEDGED_DATE,
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )

        val POSITIVE_CONFIRMED_TEST_RESULT = AcknowledgedTestResult(
            "token",
            TEST_END_DATE,
            POSITIVE,
            RAPID_RESULT,
            ACKNOWLEDGED_DATE,
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )
    }
}
