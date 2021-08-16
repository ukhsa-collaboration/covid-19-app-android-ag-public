package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider.Companion.UNACKNOWLEDGED_TEST_RESULTS_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UnacknowledgedTestResultsProviderTest :
    ProviderTest<UnacknowledgedTestResultsProvider, List<ReceivedTestResult>>() {

    private val fixedClock = Clock.fixed(Instant.parse("2020-10-07T00:05:00.00Z"), ZoneOffset.UTC)

    override val getTestSubject: (Moshi, SharedPreferences) -> UnacknowledgedTestResultsProvider =
        { moshi, sharedPreferences -> UnacknowledgedTestResultsProvider(fixedClock, moshi, sharedPreferences) }
    override val property = UnacknowledgedTestResultsProvider::testResults
    override val key = UNACKNOWLEDGED_TEST_RESULTS_KEY
    override val defaultValue: List<ReceivedTestResult> = emptyList()
    override val expectations: List<ProviderTestExpectation<List<ReceivedTestResult>>> = listOf(
        ProviderTestExpectation(
            json = SINGLE_TEST_RESULT_WITHOUT_REQUIRES_CONFIRMATORY_TEST_JSON,
            objectValue = singleTestResultWithoutRequiresConfirmatoryTest,
            direction = JSON_TO_OBJECT
        ),
        ProviderTestExpectation(
            json = MULTIPLE_TEST_RESULTS_JSON,
            objectValue = multipleTestResults,
            direction = JSON_TO_OBJECT
        )
    )

    @Test
    fun `add PCR test result`() {
        testSubject.add(SINGLE_RECEIVED_LAB_RESULT_TEST_RESULT)

        assertSharedPreferenceSetsValue(SINGLE_LAB_RESULT_TEST_RESULT_JSON)
    }

    @Test
    fun `add assisted LFD test result`() {
        testSubject.add(SINGLE_RECEIVED_RAPID_RESULT_TEST_RESULT)

        assertSharedPreferenceSetsValue(SINGLE_RAPID_RESULT_TEST_RESULT_JSON)
    }

    @Test
    fun `add unassisted LFD test result`() {
        testSubject.add(SINGLE_RECEIVED_RAPID_SELF_REPORTED_TEST_RESULT)

        assertSharedPreferenceSetsValue(SINGLE_RAPID_SELF_REPORTED_TEST_RESULT_JSON)
    }

    @Test
    fun `stores the day limit for a positive unconfirmed test result`() {
        testSubject.add(SINGLE_RAPID_RESULT_UNCONFIRMED_POSITIVE_TEST_RESULT)

        assertSharedPreferenceSetsValue(SINGLE_RAPID_RESULT_UNCONFIRMED_TEST_JSON)
    }

    @Test
    fun `set explicit symptoms onset date`() {
        sharedPreferencesReturns(SINGLE_LAB_RESULT_TEST_RESULT_JSON)

        testSubject.setSymptomsOnsetDate(
            SINGLE_RECEIVED_LAB_RESULT_TEST_RESULT,
            SymptomsDate(
                explicitDate = LocalDate.now(fixedClock)
            )
        )

        assertSharedPreferenceSetsValue(SINGLE_LAB_RESULT_TEST_RESULT_WITH_EXPLICIT_ONSET_DATE_JSON)
    }

    @Test
    fun `set cannot remember symptoms onset date`() {
        sharedPreferencesReturns(SINGLE_LAB_RESULT_TEST_RESULT_JSON)

        testSubject.setSymptomsOnsetDate(
            SINGLE_RECEIVED_LAB_RESULT_TEST_RESULT,
            SymptomsDate(
                explicitDate = null
            )
        )

        assertSharedPreferenceSetsValue(SINGLE_LAB_RESULT_TEST_RESULT_WITH_CANNOT_REMEMBER_ONSET_DATE_JSON)
    }

    @Test
    fun `remove test result`() {
        sharedPreferencesReturns(SINGLE_LAB_RESULT_TEST_RESULT_JSON)

        testSubject.remove(SINGLE_RECEIVED_LAB_RESULT_TEST_RESULT)

        assertSharedPreferenceSetsValue(EMPTY_JSON)
    }

    @Test
    fun `clear no test results`() {
        sharedPreferencesReturns(MULTIPLE_TEST_RESULTS_JSON)

        testSubject.clearBefore(LocalDate.of(1970, 1, 1))

        assertSharedPreferenceSetsValue(MULTIPLE_TEST_RESULTS_JSON.replace("\n", ""))
    }

    @Test
    fun `clear some test results`() {
        sharedPreferencesReturns(MULTIPLE_TEST_RESULTS_JSON)

        testSubject.clearBefore(LocalDate.of(1971, 1, 2))

        assertSharedPreferenceSetsValue("""[{"diagnosisKeySubmissionToken":"token3","testEndDate":"1972-01-01T00:00:00Z","testResult":"NEGATIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":true,"requiresConfirmatoryTest":false}]""")
    }

    @Test
    fun `clear all test results`() {
        sharedPreferencesReturns(MULTIPLE_TEST_RESULTS_JSON)

        testSubject.clearBefore(LocalDate.of(1972, 1, 2))

        assertSharedPreferenceSetsValue(EMPTY_JSON)
    }

    companion object {
        val SINGLE_LAB_RESULT_TEST_RESULT_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":false,"requiresConfirmatoryTest":false}]
            """.trimIndent()

        val SINGLE_LAB_RESULT_TEST_RESULT_WITH_EXPLICIT_ONSET_DATE_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":false,"requiresConfirmatoryTest":false,"symptomsOnsetDate":{"explicitDate":"2020-10-07"}}]
            """.trimIndent()

        val SINGLE_LAB_RESULT_TEST_RESULT_WITH_CANNOT_REMEMBER_ONSET_DATE_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":false,"requiresConfirmatoryTest":false,"symptomsOnsetDate":{}}]
            """.trimIndent()

        val SINGLE_RAPID_RESULT_TEST_RESULT_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"RAPID_RESULT","diagnosisKeySubmissionSupported":false,"requiresConfirmatoryTest":true}]
            """.trimIndent()

        val SINGLE_TEST_RESULT_WITHOUT_REQUIRES_CONFIRMATORY_TEST_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"RAPID_RESULT","diagnosisKeySubmissionSupported":false}]
            """.trimIndent()

        val SINGLE_RAPID_SELF_REPORTED_TEST_RESULT_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","diagnosisKeySubmissionSupported":false,"requiresConfirmatoryTest":true}]
            """.trimIndent()

        val SINGLE_RAPID_RESULT_UNCONFIRMED_TEST_JSON =
            """
            [{"diagnosisKeySubmissionToken":"token","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"RAPID_RESULT","diagnosisKeySubmissionSupported":true,"requiresConfirmatoryTest":true,"confirmatoryDayLimit":0}]
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_JSON =
            """
            [
            {"diagnosisKeySubmissionToken":"token1","testEndDate":"1970-01-01T00:00:00Z","testResult":"POSITIVE","testKitType":"RAPID_SELF_REPORTED","diagnosisKeySubmissionSupported":false,"requiresConfirmatoryTest":true},
            {"diagnosisKeySubmissionToken":"token2","testEndDate":"1971-01-01T00:00:00Z","testResult":"VOID","testKitType":"RAPID_RESULT","diagnosisKeySubmissionSupported":true,"requiresConfirmatoryTest":true},
            {"diagnosisKeySubmissionToken":"token3","testEndDate":"1972-01-01T00:00:00Z","testResult":"NEGATIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":true,"requiresConfirmatoryTest":false}
            ]
            """.trimIndent()

        val MULTIPLE_TEST_RESULTS_NONE_POSITIVE_JSON =
            """
            [
            {"diagnosisKeySubmissionToken":"token2","testEndDate":"1971-01-01T00:00:00Z","testResult":"VOID","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":true,"requiresConfirmatoryTest":false},
            {"diagnosisKeySubmissionToken":"token3","testEndDate":"1972-01-01T00:00:00Z","testResult":"NEGATIVE","testKitType":"LAB_RESULT","diagnosisKeySubmissionSupported":true,"requiresConfirmatoryTest":false}
            ]
            """.trimIndent()

        val EMPTY_JSON =
            """
            []
            """.trimIndent()

        val SINGLE_RECEIVED_LAB_RESULT_TEST_RESULT = ReceivedTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE,
            LAB_RESULT,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = false
        )

        val SINGLE_RECEIVED_RAPID_RESULT_TEST_RESULT = ReceivedTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = true
        )

        val SINGLE_RECEIVED_RAPID_SELF_REPORTED_TEST_RESULT = ReceivedTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE,
            RAPID_SELF_REPORTED,
            diagnosisKeySubmissionSupported = false,
            requiresConfirmatoryTest = true
        )

        val SINGLE_RAPID_RESULT_UNCONFIRMED_POSITIVE_TEST_RESULT = ReceivedTestResult(
            "token",
            Instant.ofEpochMilli(0),
            POSITIVE,
            RAPID_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true,
            confirmatoryDayLimit = 0
        )
        private val singleTestResultWithoutRequiresConfirmatoryTest = listOf(
            ReceivedTestResult(
                "token",
                Instant.parse("1970-01-01T00:00:00Z"),
                POSITIVE,
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = false,
                requiresConfirmatoryTest = false
            )
        )
        private val multipleTestResults = listOf(
            ReceivedTestResult(
                "token1",
                Instant.parse("1970-01-01T00:00:00Z"),
                POSITIVE,
                RAPID_SELF_REPORTED,
                diagnosisKeySubmissionSupported = false,
                requiresConfirmatoryTest = true
            ),
            ReceivedTestResult(
                "token2",
                Instant.parse("1971-01-01T00:00:00Z"),
                VOID,
                RAPID_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = true
            ),
            ReceivedTestResult(
                "token3",
                Instant.parse("1972-01-01T00:00:00Z"),
                NEGATIVE,
                LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )
    }
}
