package uk.nhs.nhsx.covid19.android.app.battery

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BatteryOptimizationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class BatteryOptimizationActivityTest : EspressoTest() {

    private val batteryOptimizationRobot = BatteryOptimizationRobot()
    private val statusRobot = StatusRobot()

    @Before
    fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)
    }

    @After
    fun tearDown() {
        testAppContext.setIgnoringBatteryOptimizations(false)
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun showScreen() {
        startTestActivity<BatteryOptimizationActivity>()

        batteryOptimizationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun openingBatteryOptimizationActivityWhenAlreadyIgnoringBatteryOptimizations_shouldStartStatusActivity() {
        testAppContext.setIgnoringBatteryOptimizations(true)

        startTestActivity<BatteryOptimizationActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickAllowAndPressBack_shouldRemainInBatteryOptimizationActivity() {
        startTestActivity<BatteryOptimizationActivity>()

        batteryOptimizationRobot.clickAllowButton()

        testAppContext.device.pressBack()

        batteryOptimizationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun pressBack_shouldRemainInBatteryOptimizationActivity() {
        startTestActivity<BatteryOptimizationActivity>()

        testAppContext.device.pressBack()

        batteryOptimizationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun pressClose_shouldStartStatusActivity() {
        startTestActivity<BatteryOptimizationActivity>()

        batteryOptimizationRobot.clickCloseButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
