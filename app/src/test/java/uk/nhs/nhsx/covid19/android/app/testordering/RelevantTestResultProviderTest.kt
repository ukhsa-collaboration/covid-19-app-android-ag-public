package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED

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
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        assertTrue(testSubject.isTestResultPositive())
    }

    @Test
    fun `isTestResultPositive returns false for negative test result`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        assertFalse(testSubject.isTestResultPositive())
    }

    @Test
    fun `isTestResultPositive returns false for no test`() {
        every { relevantTestResultStorage.value } returns null

        assertFalse(testSubject.isTestResultPositive())
    }

    @Test
    fun `isTestResultNegative returns true for negative test result`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        assertTrue(testSubject.isTestResultNegative())
    }

    @Test
    fun `isTestResultNegative returns false for positive test result`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        assertFalse(testSubject.isTestResultNegative())
    }

    @Test
    fun `isTestResultNegative returns false for no test`() {
        every { relevantTestResultStorage.value } returns null

        assertFalse(testSubject.isTestResultNegative())
    }

    @Test
    fun `getTestResultIfPositive returns test if positive`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        assertEquals(POSITIVE_RAPID_RESULT_TEST_RESULT, testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive null if negative`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `getTestResultIfPositive null if no test`() {
        every { relevantTestResultStorage.value } returns null

        assertNull(testSubject.getTestResultIfPositive())
    }

    @Test
    fun `hasPositiveTestResultAfter returns true if positive after the date`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        val result = testSubject.hasPositiveTestResultAfter(Instant.parse("1969-12-31T00:00:00Z"))

        assertTrue(result)
    }

    @Test
    fun `hasPositiveTestResultAfter returns false if positive on the date`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        val result = testSubject.hasPositiveTestResultAfter(Instant.parse("1970-01-01T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `hasPositiveTestResultAfter returns false if no positive test`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        val result = testSubject.hasPositiveTestResultAfter(Instant.parse("1969-12-31T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `hasPositiveTestResultAfter returns false if no tests`() {
        every { relevantTestResultStorage.value } returns null

        val result = testSubject.hasPositiveTestResultAfter(Instant.parse("1970-01-01T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `hasPositiveTestResultAfterOrEqual returns true if positive after the date`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        val result = testSubject.hasPositiveTestResultAfterOrEqual(Instant.parse("1969-12-31T00:00:00Z"))

        assertTrue(result)
    }

    @Test
    fun `hasPositiveTestResultAfterOrEqual returns true if positive on the date`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        val result = testSubject.hasPositiveTestResultAfterOrEqual(Instant.parse("1970-01-01T00:00:00Z"))

        assertTrue(result)
    }

    @Test
    fun `hasPositiveTestResultAfterOrEqual returns false if no positive test`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        val result = testSubject.hasPositiveTestResultAfterOrEqual(Instant.parse("1969-12-31T00:00:00Z"))

        assertFalse(result)
    }

    @Test
    fun `hasPositiveTestResultAfterOrEqual returns false if no tests`() {
        every { relevantTestResultStorage.value } returns null

        val result = testSubject.hasPositiveTestResultAfterOrEqual(Instant.parse("1970-01-01T00:00:00Z"))

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
                diagnosisKeySubmissionSupported = true
            )
        )

        verify(exactly = 0) { relevantTestResultStorage setProperty "value" value any<String>() }
    }

    @Test
    fun `onTestResultAcknowledged updates with negative test result if no previous test result`() {
        every { relevantTestResultStorage.value } returns null

        val testEndDate = Instant.parse("1970-01-01T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.NEGATIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )

        val expectedResult =
            """
            {"diagnosisKeySubmissionToken":"newToken","testEndDate":"$testEndDate","testResult":"NEGATIVE","testKitType":"LAB_RESULT","acknowledgedDate":"${Instant.now(fixedClock)}"}
            """.trimIndent()

        verify { relevantTestResultStorage setProperty "value" value eq(expectedResult) }
    }

    @Test
    fun `onTestResultAcknowledged updates with negative test result if newer than previous negative test result`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        val testEndDate = Instant.parse("1970-01-02T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.NEGATIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )

        val expectedResult =
            """
            {"diagnosisKeySubmissionToken":"newToken","testEndDate":"$testEndDate","testResult":"NEGATIVE","testKitType":"LAB_RESULT","acknowledgedDate":"${Instant.now(fixedClock)}"}
            """.trimIndent()

        verify { relevantTestResultStorage setProperty "value" value eq(expectedResult) }
    }

    @Test
    fun `onTestResultAcknowledged does not update with negative test result if older than previous negative test result`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        val testEndDate = Instant.parse("1969-01-01T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.NEGATIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )

        verify(exactly = 0) { relevantTestResultStorage setProperty "value" value any<String>() }
    }

    @Test
    fun `onTestResultAcknowledged does not update with negative test result if previous positive test result`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        val testEndDate = Instant.parse("1971-01-01T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.NEGATIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )

        verify(exactly = 0) { relevantTestResultStorage setProperty "value" value any<String>() }
    }

    @Test
    fun `onTestResultAcknowledged updates with positive test result if no previous test result`() {
        every { relevantTestResultStorage.value } returns null

        val testEndDate = Instant.parse("1970-01-01T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.POSITIVE,
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )

        val expectedResult =
            """
            {"diagnosisKeySubmissionToken":"newToken","testEndDate":"$testEndDate","testResult":"POSITIVE","testKitType":"RAPID_RESULT","acknowledgedDate":"${Instant.now(fixedClock)}"}
            """.trimIndent()

        verify { relevantTestResultStorage setProperty "value" value eq(expectedResult) }
    }

    @Test
    fun `onTestResultAcknowledged updates with positive test result if newer than previous positive test result`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_SELF_REPORTED_TEST_RESULT_JSON

        val testEndDate = Instant.parse("1970-01-02T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.POSITIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = true
            )
        )

        val expectedResult =
            """
            {"diagnosisKeySubmissionToken":"newToken","testEndDate":"$testEndDate","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","acknowledgedDate":"${Instant.now(fixedClock)}"}
            """.trimIndent()

        verify { relevantTestResultStorage setProperty "value" value eq(expectedResult) }
    }

    @Test
    fun `onTestResultAcknowledged does not update with positive test result if older than previous positive test result`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        val testEndDate = Instant.parse("1969-01-01T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.POSITIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )

        verify(exactly = 0) { relevantTestResultStorage setProperty "value" value any<String>() }
    }

    @Test
    fun `onTestResultAcknowledged updates with positive test result if previous negative test result`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        val testEndDate = Instant.parse("1960-01-01T00:00:00Z")
        testSubject.onTestResultAcknowledged(
            ReceivedTestResult(
                "newToken",
                testEndDate,
                VirologyTestResult.POSITIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true
            )
        )

        val expectedResult =
            """
            {"diagnosisKeySubmissionToken":"newToken","testEndDate":"$testEndDate","testResult":"POSITIVE","testKitType":"LAB_RESULT","acknowledgedDate":"${Instant.now(fixedClock)}"}
            """.trimIndent()

        verify { relevantTestResultStorage setProperty "value" value eq(expectedResult) }
    }

    @Test
    fun `clear test result`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        testSubject.clear()

        verify { relevantTestResultStorage setProperty "value" value null }
    }

    @Test
    fun `with positive assisted LFD test storage`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_RESULT_TEST_RESULT_JSON

        assertEquals(POSITIVE_RAPID_RESULT_TEST_RESULT, testSubject.testResult)
    }

    @Test
    fun `with positive unassisted LFD test storage`() {
        every { relevantTestResultStorage.value } returns POSITIVE_RAPID_SELF_REPORTED_TEST_RESULT_JSON

        assertEquals(POSITIVE_RAPID_SELF_REPORTED_TEST_RESULT, testSubject.testResult)
    }

    @Test
    fun `with negative test storage`() {
        every { relevantTestResultStorage.value } returns NEGATIVE_TEST_RESULT_JSON

        assertEquals(NEGATIVE_TEST_RESULT, testSubject.testResult)
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
        val TEST_END_DATE: Instant = Instant.parse("1970-01-01T00:00:00Z")
        val ACKNOWLEDGED_DATE: Instant = Instant.parse("2020-07-26T10:00:00Z")

        val POSITIVE_RAPID_RESULT_TEST_RESULT_JSON =
            """
           {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"RAPID_RESULT","acknowledgedDate":"$ACKNOWLEDGED_DATE"}
            """.trimIndent()

        val POSITIVE_RAPID_SELF_REPORTED_TEST_RESULT_JSON =
            """
           {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","acknowledgedDate":"$ACKNOWLEDGED_DATE"}
            """.trimIndent()

        val NEGATIVE_TEST_RESULT_JSON =
            """
            {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"NEGATIVE","testKitType":"LAB_RESULT","acknowledgedDate":"$ACKNOWLEDGED_DATE"}
            """.trimIndent()

        val POSITIVE_RAPID_RESULT_TEST_RESULT = AcknowledgedTestResult(
            "token",
            TEST_END_DATE,
            POSITIVE,
            RAPID_RESULT,
            ACKNOWLEDGED_DATE
        )

        val POSITIVE_RAPID_SELF_REPORTED_TEST_RESULT = AcknowledgedTestResult(
            "token",
            TEST_END_DATE,
            POSITIVE,
            RAPID_SELF_REPORTED,
            ACKNOWLEDGED_DATE
        )

        val NEGATIVE_TEST_RESULT = AcknowledgedTestResult(
            "token",
            TEST_END_DATE,
            NEGATIVE,
            LAB_RESULT,
            ACKNOWLEDGED_DATE
        )
    }
}
