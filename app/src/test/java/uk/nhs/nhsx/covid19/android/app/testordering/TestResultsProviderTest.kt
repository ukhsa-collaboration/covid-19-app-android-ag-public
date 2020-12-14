package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestResultsProviderTest {

    private val latestResultsProvider = mockk<LatestTestResultProvider>(relaxed = true)
    private val testResultsStorage = mockk<TestResultsStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()
    private val clock = Clock.fixed(Instant.parse("2020-07-26T10:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `migration from LatestTestResultProvider`() {
        val latestTestResult = LatestTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE
        )
        every { latestResultsProvider.latestTestResult } returns latestTestResult

        TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }
        verify { latestResultsProvider.latestTestResult = null }
        verify { testResultsStorage.value = SINGLE_TEST_RESULT_JSON }
    }

    @Test
    fun `no migration needed from LatestTestResultProvider`() {
        every { latestResultsProvider.latestTestResult } returns null

        TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }
        verify(exactly = 0) { latestResultsProvider.latestTestResult = any() }
        verify(exactly = 0) { testResultsStorage.value = any() }
    }

    @Test
    fun `finding test result`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }

        val receivedTestResult = testSubject.find("token")
        val expectedTestResult = SINGLE_RECEIVED_TEST_RESULT
        assertEquals(expectedTestResult, receivedTestResult)
    }

    @Test
    fun `adding test result`() {
        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        testSubject.add(SINGLE_RECEIVED_TEST_RESULT)

        verify { testResultsStorage.value = SINGLE_TEST_RESULT_JSON }
    }

    @Test
    fun `removing test result`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        testSubject.remove(SINGLE_RECEIVED_TEST_RESULT)

        verify { testResultsStorage.value = EMPTY_JSON }
    }

    @Test
    fun `clear no test results`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_VOID_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        testSubject.clearBefore(LocalDate.of(1970, 1, 1))

        verify { testResultsStorage.value = MULTIPLE_TEST_RESULTS_LAST_IS_VOID_JSON.replace("\n", "") }
    }

    @Test
    fun `clear some test results`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_VOID_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        testSubject.clearBefore(LocalDate.of(1975, 1, 1))

        verify {
            testResultsStorage.value =
                """{"token3":{"diagnosisKeySubmissionToken":"token3","testEndDate":"1975-01-01T00:00:00Z","testResult":"POSITIVE","acknowledgedDate":"2020-07-25T10:00:00Z"}}""".trimIndent()
        }
    }

    @Test
    fun `clear all test results`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_VOID_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        testSubject.clearBefore(LocalDate.of(2020, 12, 28))

        verify { testResultsStorage.value = EMPTY_JSON }
    }

    @Test
    fun `acknowledging test result`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        testSubject.acknowledge(SINGLE_RECEIVED_TEST_RESULT)

        verify { testResultsStorage.value = SINGLE_TEST_RESULT_JSON_ACKNOWLEDGED }
    }

    @Test
    fun `with empty storage`() {
        every { testResultsStorage.value } returns null

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        val receivedTestResults = testSubject.testResults

        assertEquals(mapOf(), receivedTestResults)
    }

    @Test
    fun `with corrupt storage`() {
        every { testResultsStorage.value } returns "sdsfljghsfgyldfjg"

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        val receivedTestResults = testSubject.testResults

        assertEquals(mapOf(), receivedTestResults)
    }

    @Test
    fun `isLastRelevantTestResultPositive returns false for single unacknowledged test result`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertFalse { testSubject.isLastRelevantTestResultPositive() }
    }

    @Test
    fun `isLastRelevantTestResultNegative returns false for single unacknowledged test result`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertFalse { testSubject.isLastRelevantTestResultNegative() }
    }

    @Test
    fun `isLastRelevantTestResultPositive returns true for single acknowledged positive test result`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON_ACKNOWLEDGED

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertTrue { testSubject.isLastRelevantTestResultPositive() }
    }

    @Test
    fun `isLastRelevantTestResultNegative returns true for single acknowledged negative test result`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_NEGATIVE_JSON_ACKNOWLEDGED

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertTrue { testSubject.isLastRelevantTestResultNegative() }
    }

    @Test
    fun `isLastRelevantTestResultNegative returns false when containing positive and negative acknowledged test results`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_VOID_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertFalse { testSubject.isLastRelevantTestResultNegative() }
    }

    @Test
    fun `get last non-void test result`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_VOID_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        val expectedResult = ReceivedTestResult(
            "token2",
            Instant.ofEpochMilli(0),
            NEGATIVE,
            Instant.parse("2020-07-26T10:00:00Z")
        )
        assertEquals(expectedResult, testSubject.getLastNonVoidTestResult())
    }

    @Test
    fun `has had positive test result since returns true if positive test result after date`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        val date = Instant.parse("1969-12-31T00:00:00Z")
        assertTrue(testSubject.hasHadPositiveTestSince(date))
    }

    @Test
    fun `has had positive test result since returns true if positive test result on date`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        val date = Instant.parse("1970-01-01T00:00:00Z")
        assertTrue(testSubject.hasHadPositiveTestSince(date))
    }

    @Test
    fun `has had positive test result since returns false if positive test result before date`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        val date = Instant.parse("1970-01-02T00:00:00Z")
        assertFalse(testSubject.hasHadPositiveTestSince(date))
    }

    @Test
    fun `has had positive test result since returns false if only negative or void test results`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_NONE_ARE_POSITIVE_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        val date = Instant.parse("1969-12-31T00:00:00Z")
        assertFalse(testSubject.hasHadPositiveTestSince(date))
    }

    companion object {
        val SINGLE_TEST_RESULT_JSON =
            """
            {"token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE"}}
            """.trimIndent()

        val SINGLE_TEST_RESULT_JSON_ACKNOWLEDGED =
            """
            {"token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","acknowledgedDate":"2020-07-26T10:00:00Z"}}
            """.trimIndent()

        val SINGLE_TEST_RESULT_NEGATIVE_JSON_ACKNOWLEDGED =
            """
            {"token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"NEGATIVE","acknowledgedDate":"2020-07-26T10:00:00Z"}}
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_JSON =
            """
            {
            "token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE"},
            "token2":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1975-01-01T00:00:00Z","testResult":"VOID","acknowledgedDate":"2020-07-26T10:00:00Z"}
            }
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_LAST_IS_VOID_JSON =
            """
            {
            "token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"VOID","acknowledgedDate":"2020-12-27T10:00:00Z"},
            "token2":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1970-01-01T00:00:00Z","testResult":"NEGATIVE","acknowledgedDate":"2020-07-26T10:00:00Z"},
            "token3":{"diagnosisKeySubmissionToken":"token3","testEndDate":"1975-01-01T00:00:00Z","testResult":"POSITIVE","acknowledgedDate":"2020-07-25T10:00:00Z"}
            }
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_NONE_ARE_POSITIVE_JSON =
            """
            {
            "token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"NEGATIVE"},
            "token2":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1975-01-01T00:00:00Z","testResult":"VOID","acknowledgedDate":"2020-07-26T10:00:00Z"}
            }
            """.trimIndent()

        val EMPTY_JSON =
            """
            {}
            """.trimIndent()

        val SINGLE_RECEIVED_TEST_RESULT = ReceivedTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE
        )
    }
}
