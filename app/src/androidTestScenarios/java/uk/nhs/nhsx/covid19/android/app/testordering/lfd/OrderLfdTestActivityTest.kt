package uk.nhs.nhsx.covid19.android.app.testordering.lfd

import android.app.Activity
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.OrderLfdTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

class OrderLfdTestActivityTest : EspressoTest() {

    private val orderLFDTestRobot = OrderLfdTestRobot()
    private val statusRobot = StatusRobot()
    private val browserRobot = BrowserRobot()

    var activity: Activity? = null

    @Before
    fun setUp() = runBlocking {
        activity = startTestActivity<OrderLfdTestActivity>()
        orderLFDTestRobot.checkActivityIsDisplayed()
    }

    @Test
    fun orderLFDTestButton_opensExternalBrowser() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            orderLFDTestRobot.clickOrderTestButton()

            waitFor { browserRobot.checkActivityIsDisplayed() }

            browserRobot.clickCloseButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun pressIAlreadyHaveKitButton_shouldFinishAndNavigateToStatusScreen() {
        orderLFDTestRobot.clickIAlreadyHaveKitButton()

        statusRobot.checkActivityIsDisplayed()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun pressBack_shouldFinishActivity() {
        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
