package uk.nhs.nhsx.covid19.android.app.battery

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BatteryOptimizationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithIntents

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

    @Test
    fun clickAllowAndOk_shouldStartStatusActivity() {
        runWithIntents {
            val result = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
            val packageUri = Uri.parse("package:${testAppContext.app.packageName}")
            Intents.intending(IntentMatchers.hasData(packageUri)).respondWith(result)

            startTestActivity<BatteryOptimizationActivity>()

            batteryOptimizationRobot.clickAllowButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
    }
}
