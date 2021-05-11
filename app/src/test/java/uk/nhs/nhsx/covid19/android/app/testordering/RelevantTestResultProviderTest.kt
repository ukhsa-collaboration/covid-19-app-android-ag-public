@file:Suppress("DEPRECATION")

package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RelevantTestResultProviderTest {

    private val relevantTestResultStorage = mockk<RelevantTestResultStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    private val testSubject = RelevantTestResultProvider(
        relevantTestResultStorage,
        moshi
    )

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
