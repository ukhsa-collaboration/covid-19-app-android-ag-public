package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalculateIndexExpiryDateTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-01-15T10:00:00Z"), ZoneOffset.UTC)
    private val isolationConfiguration = DurationDays()

    private val testSubject = CalculateIndexExpiryDate(fixedClock)

    //region Self-assessment and negative test
    @Test
    fun `when self-assessment with explicit onset date and negative test, use test end date`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = LocalDate.now(fixedClock).minusDays(5)
        )

        val expiryDate = testSubject(selfAssessment, negativeTestResult, isolationConfiguration)

        thenUseTestEndDateAsExpiryDate(expiryDate, negativeTestResult)
    }

    @Test
    fun `when self-assessment without explicit onset date and negative test, use test end date`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = null
        )

        val expiryDate = testSubject(selfAssessment, negativeTestResult, isolationConfiguration)

        thenUseTestEndDateAsExpiryDate(expiryDate, negativeTestResult)
    }
    //endregion

    //region Self-assessment
    @Test
    fun `when self-assessment with explicit onset date and no test, use symptoms onset date as base`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = LocalDate.now(fixedClock).minusDays(5)
        )

        val expiryDate = testSubject(selfAssessment, testResult = null, isolationConfiguration)

        thenUseSymptomsOnsetDateAsBase(expiryDate, selfAssessment)
    }

    @Test
    fun `when self-assessment with explicit onset date and positive test, use symptoms onset date as base`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = LocalDate.now(fixedClock).minusDays(5)
        )

        val expiryDate = testSubject(selfAssessment, positiveTestResult, isolationConfiguration)

        thenUseSymptomsOnsetDateAsBase(expiryDate, selfAssessment)
    }

    @Test
    fun `when self-assessment without explicit onset date and no test, use self-assessment date as base`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = null
        )

        val expiryDate = testSubject(selfAssessment, testResult = null, isolationConfiguration)

        thenUseSelfAssessmentDateAsBase(expiryDate, selfAssessment)
    }

    @Test
    fun `when self-assessment without explicit onset date and positive test, use self-assessment date as base`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = null
        )

        val expiryDate = testSubject(selfAssessment, positiveTestResult, isolationConfiguration)

        thenUseSelfAssessmentDateAsBase(expiryDate, selfAssessment)
    }
    //endregion

    //region Positive test
    @Test
    fun `when no self-assessment and positive test, use test end date as base`() {
        val expiryDate = testSubject(selfAssessment = null, positiveTestResult, isolationConfiguration)

        thenUseTestEndDateAsBase(expiryDate, positiveTestResult)
    }
    //endregion

    //region No index isolation
    @Test
    fun `when no self-assessment and negative test, return null`() {
        val expiryDate = testSubject(selfAssessment = null, negativeTestResult, isolationConfiguration)

        assertNull(expiryDate)
    }

    @Test
    fun `when no self-assessment and no test, return null`() {
        val expiryDate = testSubject(selfAssessment = null, testResult = null, isolationConfiguration)

        assertNull(expiryDate)
    }
    //region

    //region Test helpers
    private fun thenUseTestEndDateAsExpiryDate(expiryDate: LocalDate?, testResult: AcknowledgedTestResult) {
        assertEquals(testResult.testEndDate, expiryDate)
    }

    private fun thenUseTestEndDateAsBase(expiryDate: LocalDate?, testResult: AcknowledgedTestResult) {
        val expectedExpiryDate = testResult.testEndDate
            .plusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong())

        assertEquals(expectedExpiryDate, expiryDate)
    }

    private fun thenUseSymptomsOnsetDateAsBase(expiryDate: LocalDate?, selfAssessment: SelfAssessment) {
        val expectedExpiryDate = selfAssessment.onsetDate!!
            .plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())

        assertEquals(expectedExpiryDate, expiryDate)
    }

    private fun thenUseSelfAssessmentDateAsBase(expiryDate: LocalDate?, selfAssessment: SelfAssessment) {
        val expectedExpiryDate = selfAssessment.selfAssessmentDate
            .plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong())

        assertEquals(expectedExpiryDate, expiryDate)
    }

    private val positiveTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock).minusDays(3),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(2)
    )

    private val negativeTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock).minusDays(3),
        testResult = NEGATIVE,
        testKitType = LAB_RESULT,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
    )
    //endregion
}
