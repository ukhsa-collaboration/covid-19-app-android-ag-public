package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.NewGuidanceForSymptomaticCasesEnglandRobot
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsDidUpdateIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenHasSymptomsNoEffectOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.IndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenIsolationDueToSelfAssessment
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.IsolationSymptomAdvice.NoIndexCaseThenSelfAssessmentNoImpactOnIsolation
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

class SymptomsAdviceIsolateScenarioTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val statusRobot = StatusRobot()
    private val guidanceForSymptomaticCasesEnglandRobot = NewGuidanceForSymptomaticCasesEnglandRobot()

    private val remainingDaysInIsolation = 7

    @Test
    fun whenNoIndexCaseThenIsolationDueToSelfAssessment_pressContinueButtonAndBack_forWales() {
        givenLocalAuthorityIsInWales()
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(
                SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_SYMPTOM_ADVICE,
                NoIndexCaseThenIsolationDueToSelfAssessment(remainingDaysInIsolation)
            )
        }
        symptomsAdviceIsolateRobot.clickBottomActionButton()
        testAppContext.device.pressBack()
        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenNoIndexCaseThenIsolationDueToSelfAssessment_pressContinueButton_forEngland() {
        givenLocalAuthorityIsInEngland()
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(
                SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_SYMPTOM_ADVICE,
                NoIndexCaseThenIsolationDueToSelfAssessment(remainingDaysInIsolation)
            )
        }
        symptomsAdviceIsolateRobot.clickBottomActionButton()

        guidanceForSymptomaticCasesEnglandRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenNoIndexCaseThenSelfAssessmentNoImpactOnIsolation_pressBackToHomeButtonShouldNavigateToStatusActivity() {
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(
                SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_SYMPTOM_ADVICE,
                NoIndexCaseThenSelfAssessmentNoImpactOnIsolation(remainingDaysInIsolation)
            )
        }
        symptomsAdviceIsolateRobot.clickBottomActionButton()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenIndexCaseThenHasSymptomsDidUpdateIsolation_pressBackToHomeButtonShouldNavigateToStatusActivity() {
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(
                SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_SYMPTOM_ADVICE,
                IndexCaseThenHasSymptomsDidUpdateIsolation(remainingDaysInIsolation)
            )
        }
        symptomsAdviceIsolateRobot.clickBottomActionButton()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenIndexCaseThenHasSymptomsNoEffectOnIsolation_pressBackToHomeButtonShouldNavigateToStatusActivity() {
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(
                SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_SYMPTOM_ADVICE,
                IndexCaseThenHasSymptomsNoEffectOnIsolation
            )
        }
        symptomsAdviceIsolateRobot.clickBottomActionButton()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenIndexCaseThenNoSymptoms_pressBackToHomeButtonShouldNavigateToStatusActivity() {
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(
                SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_SYMPTOM_ADVICE,
                IndexCaseThenNoSymptoms
            )
        }
        symptomsAdviceIsolateRobot.clickBottomActionButton()

        statusRobot.checkActivityIsDisplayed()
    }
}
