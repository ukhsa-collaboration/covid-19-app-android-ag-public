package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider.Companion.TEST_RESULTS_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Instant

@Suppress("DEPRECATION")
class TestResultsProviderTest : ProviderTest<TestResultsProvider, Map<String, OldTestResult>>() {

    override val getTestSubject = ::TestResultsProvider
    override val property = TestResultsProvider::testResults
    override val key = TEST_RESULTS_KEY
    override val defaultValue: Map<String, OldTestResult> = emptyMap()
    override val expectations: List<ProviderTestExpectation<Map<String, OldTestResult>>> = listOf(
        ProviderTestExpectation(json = MULTIPLE_TEST_RESULTS_JSON, objectValue = multipleTestResults, direction = JSON_TO_OBJECT)
    )

    @Test
    fun `clear sets storage to default value`() {
        testSubject.clear()

        assertSharedPreferenceSetsValue(null)
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
        private val multipleTestResults = mapOf(
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
    }
}
