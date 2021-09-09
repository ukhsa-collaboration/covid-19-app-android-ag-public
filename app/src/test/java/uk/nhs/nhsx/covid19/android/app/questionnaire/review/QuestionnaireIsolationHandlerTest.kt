package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoIsolationSymptomAdvice.NoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class QuestionnaireIsolationHandlerTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val riskCalculator = mockk<RiskCalculator>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val isolationHelper = IsolationLogicalHelper(fixedClock)

    private val testSubject = QuestionnaireIsolationHandler(
        isolationStateMachine,
        analyticsEventProcessor,
        riskCalculator,
        fixedClock
    )

    private val riskThreshold = 1.0f
    private val selectedSymptoms = listOf(
        Symptom(
            title = TranslatableString(mapOf()),
            description = TranslatableString(mapOf()),
            riskWeight = 1.0
        )
    )
    private val noSymptoms = listOf<Symptom>()

    private val onsetDate = SelectedDate.CannotRememberDate
    private val acknowledgedTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock),
        testResult = POSITIVE,
        testKitType = mockk(),
        acknowledgedDate = mockk()
    )

    @Test
    fun `compute advice given user is not isolating due to positive test and has symptoms then isolates due to self assessment`() {
        val selfAssessmentIndexCase = isolationHelper.selfAssessment(LocalDate.now(fixedClock)).asIsolation()
        val expectedRemainingDaysInIsolation = 7

        every { riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold) } returns true
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.neverInIsolation() andThen
            selfAssessmentIndexCase
        every { isolationStateMachine.remainingDaysInIsolation(selfAssessmentIndexCase) } returns
                expectedRemainingDaysInIsolation.toLong()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, selectedSymptoms, onsetDate)

        assertEquals(NoIndexCaseThenIsolationDueToSelfAssessment(expectedRemainingDaysInIsolation), symptomAdvice)

        verify {
            isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))
            analyticsEventProcessor.track(CompletedQuestionnaireAndStartedIsolation)
        }
    }

    @Test
    fun `compute advice given user is not isolating due to positive test and has symptoms then isolates without self assessment`() {
        val contactCaseIsolation = isolationHelper.contactCase().asIsolation()
        val expectedRemainingDaysInIsolation = 7

        every { riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold) } returns true
        every { isolationStateMachine.readLogicalState() } returns contactCaseIsolation
        every { isolationStateMachine.remainingDaysInIsolation(contactCaseIsolation) } returns
                expectedRemainingDaysInIsolation.toLong()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, selectedSymptoms, onsetDate)

        assertEquals(NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(expectedRemainingDaysInIsolation), symptomAdvice)

        verify {
            isolationStateMachine.processEvent(OnPositiveSelfAssessment(onsetDate))
            analyticsEventProcessor.track(CompletedQuestionnaireAndStartedIsolation)
        }
    }

    @Test
    fun `compute advice given user is not isolating due to positive test and has selected non-cardinal symptoms`() {
        every { riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold) } returns false
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.neverInIsolation()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, selectedSymptoms, onsetDate)

        verify { analyticsEventProcessor.track(CompletedQuestionnaireButDidNotStartIsolation) }
        assertEquals(NoSymptoms, symptomAdvice)
    }

    @Test
    fun `compute advice given user is not isolating due to positive test and has not selected any symptoms`() {
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.neverInIsolation()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, noSymptoms, onsetDate)

        verify { analyticsEventProcessor wasNot Called }
        assertEquals(NoSymptoms, symptomAdvice)
    }

    @Test
    fun `compute advice when user is isolating due to positive test and has symptoms with onset date after test end date`() {
        val selfAssessmentIndexCase =
            isolationHelper.selfAssessment(testResult = acknowledgedTestResult).asIsolation()
        val expectedRemainingDaysInIsolation = 7

        every { riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold) } returns true
        // Isolating due to positive test then due to self-assessment indicates that the self-assessment onset date is
        // after the test end date, thus updates isolation
        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.positiveTest(acknowledgedTestResult).asIsolation() andThen
                isolationHelper.selfAssessment(testResult = acknowledgedTestResult).asIsolation()
        every { isolationStateMachine.remainingDaysInIsolation(selfAssessmentIndexCase) } returns
                expectedRemainingDaysInIsolation.toLong()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, selectedSymptoms, onsetDate)

        assertEquals(IndexCaseThenHasSymptomsDidUpdateIsolation(expectedRemainingDaysInIsolation), symptomAdvice)
    }

    @Test
    fun `compute advice when user is isolating due to positive test and has symptoms with onset date before test end date`() {
        every { riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold) } returns true
        // No change in isolation state when isolating due to positive test indicates that the self-assessment onset
        // date is before the test end date
        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.positiveTest(acknowledgedTestResult).asIsolation()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, selectedSymptoms, onsetDate)

        assertEquals(IndexCaseThenHasSymptomsNoEffectOnIsolation, symptomAdvice)
    }

    @Test
    fun `compute advice when user is isolating due to positive test and has selected only non cardinal symptoms`() {
        every { riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold) } returns false
        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.positiveTest(acknowledgedTestResult).asIsolation()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, selectedSymptoms, onsetDate)

        assertEquals(IndexCaseThenNoSymptoms, symptomAdvice)

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }
    }

    @Test
    fun `compute advice when user is isolating due to positive test and has not selected any symptoms`() {
        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.positiveTest(acknowledgedTestResult).asIsolation()

        val symptomAdvice = testSubject.computeAdvice(riskThreshold, noSymptoms, onsetDate)

        assertEquals(IndexCaseThenNoSymptoms, symptomAdvice)

        verify(exactly = 0) { isolationStateMachine.processEvent(any()) }
    }
}
