package uk.nhs.nhsx.covid19.android.app.common.bluetooth

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ResolveInfo
import android.provider.Settings
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.EnableBluetoothRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithIntents
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(Parameterized::class)
class EnableBluetoothActivityTest(override val configuration: TestConfiguration) : EspressoTest() {

    private val enableBluetoothRobot = EnableBluetoothRobot()

    /**
     * This test bypasses the logic within [AndroidBluetoothStateProvider.getState], in favour of using
     * [AndroidBluetoothStateProvider.onReceive].
     */
    @Reported
    @Test
    fun activityIsDisplayed() = reporter(
        scenario = "Bluetooth disabled",
        title = "Displays bluetooth disabled screen",
        description = "User bluetooth disabled screen",
        kind = SCREEN
    ) {
        testAppContext.setBluetoothEnabled(false)
        startTestActivity<EnableBluetoothActivity>()
        enableBluetoothRobot.checkActivityIsDisplayed()

        step(
            stepName = "Show bluetooth disabled screen",
            stepDescription = "User bluetooth disabled screen"
        )
    }

    @Test
    fun activityFinishesDueToEnabledBluetooth() {
        testAppContext.setBluetoothEnabled(true)
        startTestActivity<EnableBluetoothActivity>()?.let {
            assertTrue(it.isFinishing)
        } ?: fail("The activity should have been created!")
    }

    @Test
    fun pressingOpenPhoneSettingsInEnableBluetoothActivity_shouldOpenSettings() {
            testAppContext.setBluetoothEnabled(false)
            testAppContext.packageManager.resolutionsByAction[Settings.ACTION_BLUETOOTH_SETTINGS] =
                ResolveInfo()

            startTestActivity<EnableBluetoothActivity>()

            runWithIntents {

                val expectedIntent = CoreMatchers.allOf(
                    IntentMatchers.hasAction(Settings.ACTION_BLUETOOTH_SETTINGS)
                )
                Intents.intending(expectedIntent)
                    .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

                enableBluetoothRobot.clickOpenPhoneSettingsButton()

                intended(expectedIntent)
            }
        }

    @Test
    fun clickingUseWithoutBluetooth_navigateToHomeScreen() {
        testAppContext.setBluetoothEnabled(false)

        startTestActivity<EnableBluetoothActivity>()

        runWithIntents {

            val expectedIntent = CoreMatchers.allOf(
                IntentMatchers.hasComponent(StatusActivity::class.java.name),
                IntentMatchers.hasFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            Intents.intending(expectedIntent)
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

            enableBluetoothRobot.clickContinueWithoutBluetooth()

            intended(expectedIntent)
        }
    }

    @Test
    fun clickingBack_navigateToHomeScreen() {
        testAppContext.setBluetoothEnabled(false)

        startTestActivity<EnableBluetoothActivity>()

        runWithIntents {

            val expectedIntent = CoreMatchers.allOf(
                IntentMatchers.hasComponent(StatusActivity::class.java.name),
                IntentMatchers.hasFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            Intents.intending(expectedIntent)
                .respondWith(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))

            pressBack()

            intended(expectedIntent)
        }
    }
}
