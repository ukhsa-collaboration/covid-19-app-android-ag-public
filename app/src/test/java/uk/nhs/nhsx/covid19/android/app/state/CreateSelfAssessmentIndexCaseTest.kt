package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class CreateSelfAssessmentIndexCaseTest {

    private val maxIsolationDays = 15
    private val isolationConfiguration = DurationDays(maxIsolation = maxIsolationDays)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationHelper(fixedClock, isolationConfiguration)

    private val testSubject = CreateSelfAssessmentIndexCase()

    @Test
    fun `create uncapped self-assessment with onset date`() {
        val currentState = isolationHelper.neverInIsolation()

        val onsetDate = LocalDate.now(fixedClock).minusDays(2)
        val selfAssessmentDate = LocalDate.now(fixedClock)
        val selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate)

        val result = testSubject(
            currentState.asLogical(),
            selfAssessment
        )

        val expectedExpiryDate = onsetDate.plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())
        val expectedIndexCase = IndexCase(
            isolationTrigger = selfAssessment,
            expiryDate = expectedExpiryDate
        )
        assertEquals(expectedIndexCase, result)
    }

    @Test
    fun `create uncapped self-assessment without onset date`() {
        val currentState = isolationHelper.neverInIsolation()

        val selfAssessmentDate = LocalDate.now(fixedClock)
        val selfAssessment = SelfAssessment(selfAssessmentDate)

        val result = testSubject(
            currentState.asLogical(),
            selfAssessment
        )

        val expectedExpiryDate = selfAssessmentDate.plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong())
        val expectedIndexCase = IndexCase(
            isolationTrigger = selfAssessment,
            expiryDate = expectedExpiryDate
        )
        assertEquals(expectedIndexCase, result)
    }

    @Test
    fun `create capped self-assessment with onset date`() {
        val contactCase = isolationHelper.contactCase(
            exposureDate = LocalDate.now(fixedClock).minusDays(8)
        )
        val currentState = contactCase.asIsolation(isolationConfiguration = isolationConfiguration)

        val onsetDate = LocalDate.now(fixedClock).minusDays(2)
        val selfAssessmentDate = LocalDate.now(fixedClock)
        val selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate)

        val result = testSubject(
            currentState.asLogical(),
            selfAssessment
        )

        // Since the contact case started earlier and the self-assessment will exceed the maximum duration, it will be capped
        val expectedExpiryDate = contactCase.startDate.plusDays(maxIsolationDays.toLong())
        val expectedIndexCase = IndexCase(
            isolationTrigger = selfAssessment,
            expiryDate = expectedExpiryDate
        )
        assertEquals(expectedIndexCase, result)
    }

    @Test
    fun `create capped self-assessment without onset date`() {
        val contactCase = isolationHelper.contactCase(
            exposureDate = LocalDate.now(fixedClock).minusDays(8)
        )
        val currentState = contactCase.asIsolation(isolationConfiguration = isolationConfiguration)

        val selfAssessmentDate = LocalDate.now(fixedClock)
        val selfAssessment = SelfAssessment(selfAssessmentDate)

        val result = testSubject(
            currentState.asLogical(),
            selfAssessment
        )

        // Since the contact case started earlier and the self-assessment will exceed the maximum duration, it will be capped
        val expectedExpiryDate = contactCase.startDate.plusDays(maxIsolationDays.toLong())
        val expectedIndexCase = IndexCase(
            isolationTrigger = selfAssessment,
            expiryDate = expectedExpiryDate
        )
        assertEquals(expectedIndexCase, result)
    }
}
