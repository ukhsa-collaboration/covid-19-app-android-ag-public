package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class QuestionnaireIsolationHandlerTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val analyticsEventTracker = mockk<AnalyticsEventTracker>(relaxUnitFun = true)
    private val riskCalculator = mockk<RiskCalculator>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject = QuestionnaireIsolationHandler(
        isolationStateMachine,
        analyticsEventTracker,
        riskCalculator,
        fixedClock
    )

    private val riskThreshold = 1.0f
    private val symptoms = listOf<Symptom>()
    private val onsetDate = SelectedDate.CannotRememberDate
    private val acknowledgedTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock),
        testResult = POSITIVE,
        testKitType = mockk(),
        acknowledgedDate = mockk()
    )

    @Test
    fun `compute advice given user is not isolating due to positive test and has symptoms then isolates due to self assessment`() {
        val selfAssessmentIndexCase =
            isolationHelper.selfAssessment(LocalDate.now(fixedClock)).asIsolation().asLogical()
        val expectedRemainingDaysInIsolation = 7

        every { riskCalculator.isRiskAboveThreshold(symptoms, riskThreshold) } returns true
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.neverInIsolation()
            .asLogical() andThen selfAssessmentIndexCase
        every { isolationStateMachine.remainingDaysInIsolation(selfAssessmentIndexCase) } returns
            expectedRemainingDaysInIsolation.toLong()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, symptoms, onsetDate)

        assertEquals(NoIndexCaseThenIsolationDueToSelfAssessment(expectedRemainingDaysInIsolation), symptomAdvice)

        verify {
            isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))
            analyticsEventTracker.track(CompletedQuestionnaireAndStartedIsolation)
        }
    }

    @Test
    fun `compute advice given user is not isolating due to positive test and has symptoms then isolates without self assessment`() {
        val contactCaseIsolation = isolationHelper.contactCase().asIsolation().asLogical()
        val expectedRemainingDaysInIsolation = 7

        every { riskCalculator.isRiskAboveThreshold(symptoms, riskThreshold) } returns true
        every { isolationStateMachine.readLogicalState() } returns contactCaseIsolation
        every { isolationStateMachine.remainingDaysInIsolation(contactCaseIsolation) } returns
            expectedRemainingDaysInIsolation.toLong()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, symptoms, onsetDate)

        assertEquals(NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(expectedRemainingDaysInIsolation), symptomAdvice)

        verify {
            isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))
            analyticsEventTracker.track(CompletedQuestionnaireAndStartedIsolation)
        }
    }

    @Test
    fun `compute advice given user is not isolating due to positive test and has no symptoms`() {
        every { riskCalculator.isRiskAboveThreshold(symptoms, riskThreshold) } returns false
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.neverInIsolation().asLogical()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, symptoms, onsetDate)

        coVerify { analyticsEventTracker.track(CompletedQuestionnaireButDidNotStartIsolation) }
        assertEquals(NoIsolationSymptomAdvice.NoSymptoms, symptomAdvice)
    }

    @Test
    fun `compute advice when user is isolating due to positive test and has symptoms with onset date after test end date`() {
        val selfAssessmentIndexCase =
            isolationHelper.selfAssessment(testResult = acknowledgedTestResult).asIsolation().asLogical()
        val expectedRemainingDaysInIsolation = 7

        every { riskCalculator.isRiskAboveThreshold(symptoms, riskThreshold) } returns true
        // Isolating due to positive test then due to self-assessment indicates that the self-assessment onset date is
        // after the test end date, thus updates isolation
        every { isolationStateMachine.readLogicalState() } returns
            isolationHelper.positiveTest(acknowledgedTestResult).asIsolation().asLogical() andThen
            isolationHelper.selfAssessment(testResult = acknowledgedTestResult).asIsolation().asLogical()
        every { isolationStateMachine.remainingDaysInIsolation(selfAssessmentIndexCase) } returns
            expectedRemainingDaysInIsolation.toLong()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, symptoms, onsetDate)

        assertEquals(IndexCaseThenHasSymptomsDidUpdateIsolation(expectedRemainingDaysInIsolation), symptomAdvice)
    }

    @Test
    fun `compute advice when user is isolating due to positive test and has symptoms with onset date before test end date`() {
        every { riskCalculator.isRiskAboveThreshold(symptoms, riskThreshold) } returns true
        // No change in isolation state when isolating due to positive test indicates that the self-assessment onset
        // date is before the test end date
        every { isolationStateMachine.readLogicalState() } returns
            isolationHelper.positiveTest(acknowledgedTestResult).asIsolation().asLogical()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, symptoms, onsetDate)

        assertEquals(IndexCaseThenHasSymptomsNoEffectOnIsolation, symptomAdvice)
    }

    @Test
    fun `compute advice when user is isolating due to positive test and has no symptoms then `() {
        every { riskCalculator.isRiskAboveThreshold(symptoms, riskThreshold) } returns false
        every { isolationStateMachine.readLogicalState() } returns
            isolationHelper.positiveTest(acknowledgedTestResult).asIsolation().asLogical()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, symptoms, onsetDate)

        assertEquals(IndexCaseThenNoSymptoms, symptomAdvice)

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }
    }
}
