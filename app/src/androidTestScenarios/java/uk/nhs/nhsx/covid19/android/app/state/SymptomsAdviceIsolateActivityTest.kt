package uk.nhs.nhsx.covid19.android.app.state

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot

class SymptomsAdviceIsolateActivityTest : EspressoTest() {

    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()

    private val remainingDaysInIsolation = 14

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun whenNotIsolating_thenTransitionToIndexCaseDueToSelfAssessment() {
        verifyViewState(NoIndexCaseThenIsolationDueToSelfAssessment(remainingDaysInIsolation))
    }

    @Test
    fun whenIsolatingAsContactCase_thenReportSymptomsWithoutTransition() {
        verifyViewState(NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(remainingDaysInIsolation))
    }

    @Test
    fun whenIsolatingDueToPositiveTestResult_thenReportSymptomsWithOnsetDateAfterTestResult() {
        verifyViewState(IndexCaseThenHasSymptomsDidUpdateIsolation(remainingDaysInIsolation))
    }

    @Test
    fun whenIsolatingDueToPositiveTestResult_thenReportSymptomsWithOnsetDateBeforeTestResult() {
        verifyViewState(IndexCaseThenHasSymptomsNoEffectOnIsolation)
    }

    @Test
    fun whenIsolatingDueToPositiveTestResult_thenReportNoSymptoms() {
        verifyViewState(IndexCaseThenNoSymptoms)
    }

    private fun verifyViewState(isolationSymptomAdvice: IsolationSymptomAdvice) {
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(
                SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_SYMPTOM_ADVICE,
                isolationSymptomAdvice
            )
        }
        symptomsAdviceIsolateRobot.checkActivityIsDisplayed()
        symptomsAdviceIsolateRobot.checkViewState(isolationSymptomAdvice)
    }
}
