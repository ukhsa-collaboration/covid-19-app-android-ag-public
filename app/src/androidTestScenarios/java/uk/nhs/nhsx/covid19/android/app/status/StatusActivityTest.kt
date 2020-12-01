package uk.nhs.nhsx.covid19.android.app.status

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationReminderRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrScannerRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class StatusActivityTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val qrScannerRobot = QrScannerRobot()
    private val exposureNotificationReminderRobot = ExposureNotificationReminderRobot()

    @Test
    fun clickMoreAboutApp() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickMoreAboutApp()

        moreAboutAppRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickVenueCheckIn() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickVenueCheckIn()

        qrScannerRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickEncounterDetectionSwitch() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickEncounterDetectionSwitch()

        exposureNotificationReminderRobot.checkDialogIsDisplayed()

        setScreenOrientation(LANDSCAPE)

        waitFor { exposureNotificationReminderRobot.checkDialogIsDisplayed() }

        setScreenOrientation(PORTRAIT)

        waitFor { exposureNotificationReminderRobot.checkDialogIsDisplayed() }

        waitFor { exposureNotificationReminderRobot.clickDontRemindMe() }

        waitFor { statusRobot.checkEncounterDetectionSwitchIsNotChecked() }

        statusRobot.clickEncounterDetectionSwitch()

        statusRobot.checkEncounterDetectionSwitchIsChecked()
    }
}
