package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.CONTINUE_NORMAL_ACTIVITIES
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.TRY_TO_STAY_AT_HOME
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.CheckYourAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomCheckerAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

class SymptomCheckerAdviceActivityTest : EspressoTest() {
    private val robot = SymptomCheckerAdviceRobot()
    private val browserRobot = BrowserRobot()
    private val statusActivityRobot = StatusRobot()
    private val checkYourAnswersRobot = CheckYourAnswersRobot()

    @Test
    fun whenShouldContinueWithNormalActivities_stateIsDisplayed() {
        startActivityWithExtras(CONTINUE_NORMAL_ACTIVITIES)
        waitFor { robot.checkActivityIsDisplayed() }
        waitFor { robot.checkContinueNormalActivitiesIsDisplayed() }
    }

    @Test
    fun whenShouldTryToStayAtHome_stateIsDisplayed() {
        startActivityWithExtras(TRY_TO_STAY_AT_HOME)
        waitFor { robot.checkActivityIsDisplayed() }
        waitFor { robot.checkTryToStayAtHomeIsDisplayed() }
    }

    @Test
    fun whenClickNoticeLinkForTryToStayAtHome_shouldOpenInInternalBrowser() {
        startActivityWithExtras(TRY_TO_STAY_AT_HOME)
        waitFor { robot.checkActivityIsDisplayed() }

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
            robot.clickNoticeLink()
        }
        waitFor { browserRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenClickNoticeLinkForContinueNormalActivities_shouldOpenInInternalBrowser() {
        startActivityWithExtras(CONTINUE_NORMAL_ACTIVITIES)
        waitFor { robot.checkActivityIsDisplayed() }

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
            robot.clickNoticeLink()
        }
        waitFor { browserRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenClickMedicalAdviceLinkForTryToStayAtHome_shouldOpenInInternalBrowser() {
        startActivityWithExtras(TRY_TO_STAY_AT_HOME)
        waitFor { robot.checkActivityIsDisplayed() }

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
            robot.clickMedicalLink()
        }
        waitFor { browserRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenClickMedicalAdviceLinkForContinueNormalActivities_shouldOpenInInternalBrowser() {
        startActivityWithExtras(CONTINUE_NORMAL_ACTIVITIES)
        waitFor { robot.checkActivityIsDisplayed() }

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
            robot.clickMedicalLink()
        }
        waitFor { browserRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenClickBackToHome_shouldOpenStatusActivity() {
        startActivityWithExtras(TRY_TO_STAY_AT_HOME)
        waitFor { robot.checkActivityIsDisplayed() }

        robot.clickBackToHomeButton()
        waitFor { statusActivityRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun whenNavigateBack_shouldOpenCheckYourAnswersActivity() {
        startActivityWithExtras(TRY_TO_STAY_AT_HOME)
        waitFor { robot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }
    }

    private fun startActivityWithExtras(result: SymptomCheckerAdviceResult) {
        startTestActivity<SymptomCheckerAdviceActivity> {
            putExtra(
                SymptomCheckerAdviceActivity.VALUE_KEY_QUESTIONS, SymptomsCheckerQuestions(
                    null,
                    null,
                    null
                )
            )
            putExtra(SymptomCheckerAdviceActivity.VALUE_KEY_RESULT, result)
        }
    }
}
