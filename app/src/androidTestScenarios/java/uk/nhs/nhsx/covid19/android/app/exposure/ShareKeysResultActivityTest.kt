package uk.nhs.nhsx.covid19.android.app.exposure

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysResultActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class ShareKeysResultActivityTest : EspressoTest() {

    private val shareResultRobot = ShareKeysResultRobot()
    private val statusRobot = StatusRobot()

    @Before
    fun setUp() {
        startTestActivity<ShareKeysResultActivity>()
        shareResultRobot.checkActivityIsDisplayed()
    }

    @Test
    fun testBackDoesNothing() {
        testAppContext.device.pressBack()
        shareResultRobot.checkActivityIsDisplayed()
    }

    @Test
    fun testActionButtonFinishesActivity() {
        shareResultRobot.clickActionButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
