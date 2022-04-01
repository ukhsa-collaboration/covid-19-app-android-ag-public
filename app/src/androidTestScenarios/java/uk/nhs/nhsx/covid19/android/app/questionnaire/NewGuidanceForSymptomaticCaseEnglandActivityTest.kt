package uk.nhs.nhsx.covid19.android.app.questionnaire

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

class NewGuidanceForSymptomaticCaseEnglandActivityTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val newGuidanceForSymptomaticCasesEnglandRobot = NewGuidanceForSymptomaticCasesEnglandRobot()
    private val statusRobot = StatusRobot()

    @Before
    fun setUp() {
        givenLocalAuthorityIsInEngland()
    }

    @Test
    fun verifyNewGuidanceForSymptomaticCaseForEnglandIsShown() {
        startTestActivity<NewGuidanceForSymptomaticCaseEnglandActivity>()
        newGuidanceForSymptomaticCasesEnglandRobot.checkActivityIsDisplayed()
        newGuidanceForSymptomaticCasesEnglandRobot.checkGuidanceIsDisplayed()
    }

    @Test
    fun clickPrimaryActionButton_shouldNavigateToHomeScreen() {
        startTestActivity<NewGuidanceForSymptomaticCaseEnglandActivity>()

        newGuidanceForSymptomaticCasesEnglandRobot.checkActivityIsDisplayed()

        newGuidanceForSymptomaticCasesEnglandRobot.clickPrimaryActionButton()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickCommonQuestion_shouldNavigateToExternalLink() {
        startTestActivity<NewGuidanceForSymptomaticCaseEnglandActivity>()
        newGuidanceForSymptomaticCasesEnglandRobot.checkActivityIsDisplayed()

        newGuidanceForSymptomaticCasesEnglandRobot.checkCommonQuestionsUrl()
    }

    @Test
    fun clickNHSOnlineService_shouldNavigateToExternalLink() {
        startTestActivity<NewGuidanceForSymptomaticCaseEnglandActivity>()
        newGuidanceForSymptomaticCasesEnglandRobot.checkActivityIsDisplayed()

        newGuidanceForSymptomaticCasesEnglandRobot.checkNHSOnlineServiceUrl()
    }
}
