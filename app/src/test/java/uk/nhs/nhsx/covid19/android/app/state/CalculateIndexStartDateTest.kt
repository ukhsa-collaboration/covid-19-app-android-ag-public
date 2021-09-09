package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
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

class CalculateIndexStartDateTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-01-15T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = CalculateIndexStartDate()

    @Test
    fun `when self-assessment and no test, return self-assessment date`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = LocalDate.now(fixedClock).minusDays(5)
        )

        val startDate = testSubject(selfAssessment, testResult = null)

        assertEquals(selfAssessment.selfAssessmentDate, startDate)
    }

    @Test
    fun `when self-assessment and positive test, return self-assessment date`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = LocalDate.now(fixedClock).minusDays(5)
        )

        val startDate = testSubject(selfAssessment, positiveTestResult)

        assertEquals(selfAssessment.selfAssessmentDate, startDate)
    }

    @Test
    fun `when self-assessment and negative test, return self-assessment date`() {
        val selfAssessment = SelfAssessment(
            selfAssessmentDate = LocalDate.now(fixedClock).minusDays(4),
            onsetDate = LocalDate.now(fixedClock).minusDays(5)
        )

        val startDate = testSubject(selfAssessment, negativeTestResult)

        assertEquals(selfAssessment.selfAssessmentDate, startDate)
    }

    @Test
    fun `when no self-assessment and positive test, return test end date`() {
        val startDate = testSubject(selfAssessment = null, positiveTestResult)

        assertEquals(positiveTestResult.testEndDate, startDate)
    }

    @Test
    fun `when no self-assessment and negative test, return null`() {
        val startDate = testSubject(selfAssessment = null, negativeTestResult)

        assertNull(startDate)
    }

    @Test
    fun `when no self-assessment and no test, return null`() {
        val startDate = testSubject(selfAssessment = null, testResult = null)

        assertNull(startDate)
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
}
