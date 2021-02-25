package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter

@Suppress("DEPRECATION")
class TestResultsProviderTest {

    private val testResultsStorage = mockk<TestResultsStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    private val testSubject = TestResultsProvider(testResultsStorage, moshi)

    @Test
    fun `with storage`() {
        every { testResultsStorage.value } returns MULTIPLE_TEST_RESULTS_JSON

        val expectedResult = mapOf(
            "token1" to OldTestResult(
                "token1",
                Instant.parse("1970-01-01T00:00:00Z"),
                POSITIVE
            ),
            "token2" to OldTestResult(
                "token2",
                Instant.parse("1971-01-01T00:00:00Z"),
                VOID,
                Instant.parse("2020-07-26T10:00:00Z")
            ),
            "token3" to OldTestResult(
                "token3",
                Instant.parse("1972-01-01T00:00:00Z"),
                NEGATIVE,
                Instant.parse("2020-07-27T10:00:00Z")
            )
        )

        assertEquals(expectedResult, testSubject.testResults)
    }

    @Test
    fun `with empty storage`() {
        every { testResultsStorage.value } returns null

        assertEquals(mapOf(), testSubject.testResults)
    }

    @Test
    fun `with corrupt storage`() {
        every { testResultsStorage.value } returns "sdsfljghsfgyldfjg"

        assertEquals(mapOf(), testSubject.testResults)
    }

    companion object {
        val MULTIPLE_TEST_RESULTS_JSON =
            """
            {
            "token1":{"diagnosisKeySubmissionToken":"token1","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE"},
            "token2":{"diagnosisKeySubmissionToken":"token2","testEndDate":"1971-01-01T00:00:00Z","testResult":"VOID","acknowledgedDate":"2020-07-26T10:00:00Z"},
            "token3":{"diagnosisKeySubmissionToken":"token3","testEndDate":"1972-01-01T00:00:00Z","testResult":"NEGATIVE","acknowledgedDate":"2020-07-27T10:00:00Z"}
            }
            """.trimIndent()
    }
}
