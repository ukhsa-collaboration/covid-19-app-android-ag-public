package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class TestResultsProviderTest {

    private val latestResultsProvider = mockk<LatestTestResultProvider>(relaxed = true)
    private val testResultsStorage = mockk<TestResultsStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()
    private val clock = Clock.fixed(Instant.parse("2020-07-26T10:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `test migration from LatestTestResultProvider`() {
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
    fun `test no migration needed from LatestTestResultProvider`() {
        every { latestResultsProvider.latestTestResult } returns null

        TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }
        verify(exactly = 0) { latestResultsProvider.latestTestResult = any() }
        verify(exactly = 0) { testResultsStorage.value = any() }
    }

    @Test
    fun `test finding test result`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }

        val receivedTestResult = testSubject.find("token")
        val expectedTestResult = SINGLE_RECEIVED_TEST_RESULT
        assertEquals(expectedTestResult, receivedTestResult)
    }

    @Test
    fun `test adding test result`() {
        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }

        testSubject.add(SINGLE_RECEIVED_TEST_RESULT)

        verify { testResultsStorage.value = SINGLE_TEST_RESULT_JSON }
    }

    @Test
    fun `test removing test result`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }

        testSubject.remove(SINGLE_RECEIVED_TEST_RESULT)

        verify { testResultsStorage.value = EMPTY_JSON }
    }

    @Test
    fun `test clear test results`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }

        testSubject.clear()

        verify { testResultsStorage.value = null }
    }

    @Test
    fun `test acknowledging test result`() {
        every { testResultsStorage.value } returns SINGLE_TEST_RESULT_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        verify { latestResultsProvider.latestTestResult }

        testSubject.acknowledge(SINGLE_RECEIVED_TEST_RESULT)

        verify { testResultsStorage.value = SINGLE_TEST_RESULT_JSON_ACKNOWLEDGED }
    }

    @Test
    fun `test with empty storage`() {
        every { testResultsStorage.value } returns null

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        val receivedTestResults = testSubject.testResults

        assertEquals(mapOf(), receivedTestResults)
    }

    @Test
    fun `test with corrupt storage`() {
        every { testResultsStorage.value } returns "sdsfljghsfgyldfjg"

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)
        val receivedTestResults = testSubject.testResults

        assertEquals(mapOf(), receivedTestResults)
    }

    @Test
    fun `last test result is positive`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_POSITIVE_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertEquals(true, testSubject.isLastTestResultPositive())
    }

    @Test
    fun `last test result is not positive`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_NOT_POSITIVE_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertEquals(false, testSubject.isLastTestResultPositive())
    }

    @Test
    fun `last test result is negative`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_NEGATIVE_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertEquals(true, testSubject.isLastTestResultNegative())
    }

    @Test
    fun `last test result is not negative`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_LAST_IS_POSITIVE_JSON

        val testSubject = TestResultsProvider(latestResultsProvider, testResultsStorage, moshi, clock)

        assertEquals(false, testSubject.isLastTestResultNegative())
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

        val MULTIPLE_TEST_RESULTS_JSON =
            """
            {
            "token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE"},
            "token2":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1975-01-01T00:00:00Z","testResult":"VOID","acknowledgedDate":"2020-07-26T10:00:00Z"}
            }
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_LAST_IS_POSITIVE_JSON =
            """
            {
            "token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE", "acknowledgedDate":"2020-12-27T10:00:00Z"},
            "token2":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1975-01-01T00:00:00Z","testResult":"VOID","acknowledgedDate":"2020-07-26T10:00:00Z"},
            "token3":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1975-01-01T00:00:00Z","testResult":"NEGATIVE","acknowledgedDate":"2020-07-26T10:00:00Z"}
            }
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_LAST_IS_NEGATIVE_JSON =
            """
            {
            "token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"NEGATIVE", "acknowledgedDate":"2020-12-27T10:00:00Z"},
            "token2":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1975-01-01T00:00:00Z","testResult":"VOID","acknowledgedDate":"2020-07-26T10:00:00Z"},
            "token3":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1975-01-01T00:00:00Z","testResult":"POSITIVE","acknowledgedDate":"2020-07-26T10:00:00Z"}
            }
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_LAST_IS_NOT_POSITIVE_JSON =
            """
            {
            "token":{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE"},
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
