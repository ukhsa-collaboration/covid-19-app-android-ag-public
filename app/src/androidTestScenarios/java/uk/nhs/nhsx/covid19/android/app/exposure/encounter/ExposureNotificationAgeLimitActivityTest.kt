package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class ExposureNotificationAgeLimitActivityTest : EspressoTest() {

    private val ageLimitRobot = ExposureNotificationAgeLimitRobot()

    @Test
    fun whenUserClicksContinue_errorIsDisplayed() {
        startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }

        ageLimitRobot.checkErrorVisible(false)
        setScreenOrientation(LANDSCAPE)
        waitFor { ageLimitRobot.checkErrorVisible(false) }
        setScreenOrientation(PORTRAIT)
        waitFor { ageLimitRobot.checkErrorVisible(false) }

        ageLimitRobot.checkNothingSelected()
        ageLimitRobot.clickContinueButton()

        setScreenOrientation(LANDSCAPE)
        waitFor { ageLimitRobot.checkErrorVisible(true) }
        setScreenOrientation(PORTRAIT)
        waitFor { ageLimitRobot.checkErrorVisible(true) }
    }

    @Test
    fun whenUserHasMadeSelection_canChange_survivesRotation() {
        startTestActivity<ExposureNotificationAgeLimitActivity>()

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }

        ageLimitRobot.checkNothingSelected()

        ageLimitRobot.clickYesButton()
        waitFor { ageLimitRobot.checkYesSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { ageLimitRobot.checkYesSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { ageLimitRobot.checkYesSelected() }
    }
}
