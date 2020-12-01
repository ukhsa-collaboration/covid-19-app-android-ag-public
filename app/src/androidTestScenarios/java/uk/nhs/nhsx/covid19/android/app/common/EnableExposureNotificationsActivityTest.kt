package uk.nhs.nhsx.covid19.android.app.common

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EnableExposureNotificationsRobot

class EnableExposureNotificationsActivityTest : EspressoTest() {

    private val enableExposureNotificationsRobot = EnableExposureNotificationsRobot()

    @Test
    fun showScreen() {
        startTestActivity<EnableExposureNotificationsActivity>()

        enableExposureNotificationsRobot.checkActivityIsDisplayed()
    }
}
