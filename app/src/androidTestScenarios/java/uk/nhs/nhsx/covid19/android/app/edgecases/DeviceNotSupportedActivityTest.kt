package uk.nhs.nhsx.covid19.android.app.edgecases

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.DeviceNotSupportedRobot

class DeviceNotSupportedActivityTest : EspressoTest() {

    private val deviceNotSupportedRobot = DeviceNotSupportedRobot()

    @Test
    fun showScreen() {
        startTestActivity<DeviceNotSupportedActivity>()

        deviceNotSupportedRobot.checkActivityIsDisplayed()
    }
}
