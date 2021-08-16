@file:Suppress("DEPRECATION")

package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider.Companion.RELEVANT_TEST_RESULT_KEY
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Instant

class RelevantTestResultProviderTest : ProviderTest<RelevantTestResultProvider, AcknowledgedTestResult4_9?>() {

    override val getTestSubject = ::RelevantTestResultProvider
    override val property = RelevantTestResultProvider::testResult
    override val key = RELEVANT_TEST_RESULT_KEY
    override val defaultValue: AcknowledgedTestResult4_9? = null
    override val expectations: List<ProviderTestExpectation<AcknowledgedTestResult4_9?>> = listOf(
        ProviderTestExpectation(json = POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON, objectValue = POSITIVE_INDICATIVE_TEST_RESULT, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = POSITIVE_INDICATIVE_SELF_REPORTED_TEST_RESULT_JSON, objectValue = POSITIVE_INDICATIVE_SELF_REPORTED_TEST_RESULT, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = POSITIVE_CONFIRMED_TEST_RESULT_JSON, objectValue = POSITIVE_CONFIRMED_TEST_RESULT, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = NEGATIVE_CONFIRMED_TEST_RESULT_JSON, objectValue = NEGATIVE_CONFIRMED_TEST_RESULT, direction = JSON_TO_OBJECT)
    )

    @Test
    fun `clear test result`() {
        sharedPreferencesReturns(POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON)

        testSubject.clear()

        assertSharedPreferenceSetsValue(null)
    }

    @Test
    fun `sets test result`() {
        testSubject.storeMigratedTestResult(POSITIVE_INDICATIVE_TEST_RESULT)

        assertSharedPreferenceSetsValue(POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON)
    }

    companion object {
        private val TEST_END_DATE: Instant = Instant.parse("1970-01-01T00:00:00Z")
        private val ACKNOWLEDGED_DATE: Instant = Instant.parse("2020-07-26T10:00:00Z")
        private val CONFIRMED_DATE: Instant = Instant.parse("2020-07-30T10:00:00Z")

        val POSITIVE_INDICATIVE_CONFIRMED_TEST_RESULT_JSON =
            """
           {"diagnosisKeySubmissionToken":"token","testEndDate":"$TEST_END_DATE","testResult":"POSITIVE","testKitType":"RAPID_RESULT","acknowledgedDate":"$ACKNOWLEDGED_DATE","requiresConfirmatoryTest":true,"confirmedDate":"$CONFIRMED_DATE"}
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

        val POSITIVE_INDICATIVE_TEST_RESULT = AcknowledgedTestResult4_9(
            "token",
            TEST_END_DATE,
            POSITIVE,
            RAPID_RESULT,
            ACKNOWLEDGED_DATE,
            requiresConfirmatoryTest = true,
            confirmedDate = CONFIRMED_DATE
        )

        val POSITIVE_INDICATIVE_SELF_REPORTED_TEST_RESULT = AcknowledgedTestResult4_9(
            "token",
            TEST_END_DATE,
            POSITIVE,
            RAPID_SELF_REPORTED,
            ACKNOWLEDGED_DATE,
            requiresConfirmatoryTest = true
        )

        val NEGATIVE_CONFIRMED_TEST_RESULT = AcknowledgedTestResult4_9(
            "token",
            TEST_END_DATE,
            NEGATIVE,
            LAB_RESULT,
            ACKNOWLEDGED_DATE,
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )

        val POSITIVE_CONFIRMED_TEST_RESULT = AcknowledgedTestResult4_9(
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
