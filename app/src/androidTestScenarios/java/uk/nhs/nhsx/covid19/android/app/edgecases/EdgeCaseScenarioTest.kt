package uk.nhs.nhsx.covid19.android.app.edgecases

import androidx.test.espresso.NoMatchingViewException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.ignoreException
import org.awaitility.kotlin.untilAsserted
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.EnableBluetoothRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.EnableLocationRobot
import java.util.concurrent.TimeUnit.SECONDS

class EdgeCaseScenarioTest : EspressoTest() {

    private val enableBluetoothRobot = EnableBluetoothRobot()
    private val enableLocationRobot = EnableLocationRobot()
    private val statusRobot = StatusRobot()

    @Test
    fun openingHomeActivityWithBluetoothDisabled_shouldNavigateToEnableBluetoothActivity() =
        reporter(
            "Edge case",
            "Bluetooth disabled",
            "The user enters the home screen but does not have bluetooth enabled.",
            Reporter.Kind.FLOW
        ) {
            testAppContext.setBluetoothEnabled(false)

            startTestActivity<StatusActivity>()

            enableBluetoothRobot.checkActivityIsDisplayed()

            step(
                "Start",
                "The user is presented with a screen that shows that bluetooth " +
                    "has to be enabled to use the app."
            )
        }

    @Test
    fun openingHomeActivityWithBluetoothDisabledThenEnableBluetooth_shouldNavigateBackToHomeActivity() =
        reporter(
            "Edge case",
            "Enable bluetooth from app",
            "The user enters the home screen but does not have bluetooth enabled. " +
                "The user is presented with a screen that urges them to enable bluetooth. " +
                "The user enables bluetooth and is navigated back to the home screen.",
            Reporter.Kind.FLOW
        ) {
            testAppContext.setBluetoothEnabled(false)

            startTestActivity<StatusActivity>()

            enableBluetoothRobot.checkActivityIsDisplayed()

            step(
                "Start",
                "The user is presented with a screen that shows that bluetooth " +
                    "has to be enabled to use the app."
            )

            testAppContext.setBluetoothEnabled(true)

            step(
                "Bluetooth enabled",
                "After successfully enabling bluetooth, the app navigates back to " +
                    "the home screen."
            )

            statusRobot.checkActivityIsDisplayed()
        }

    @Test
    fun openingHomeActivityWithLocationServicesDisabled_shouldNavigateToEnableLocationActivity() =
        reporter(
            "Edge case",
            "Location services disabled",
            "The user enters the home screen but does not have location services enabled.",
            Reporter.Kind.FLOW
        ) {
            testAppContext.setLocationEnabled(false)

            startTestActivity<StatusActivity>()

            enableLocationRobot.checkActivityIsDisplayed()

            step(
                "Start",
                "The user is presented with a screen that shows that location " +
                    "services have to be enabled to use the app."
            )
        }

    @Test
    fun openingHomeActivityWithLocationServicesDisabledThenEnableLocationServices_shouldNavigateBackToHomeActivity() =
        reporter(
            "Edge case",
            "Enable location services from app",
            "The user enters the home screen but does not have location services enabled. " +
                "The user is presented with a screen that urges them to enable location services. " +
                "The user enables location services and is navigated back to the home screen.",
            Reporter.Kind.FLOW
        ) {
            testAppContext.setLocationEnabled(false)

            startTestActivity<StatusActivity>()

            enableLocationRobot.checkActivityIsDisplayed()

            step(
                "Start",
                "The user is presented with a screen that shows that location services " +
                    "has to be enabled to use the app."
            )

            testAppContext.setLocationEnabled(true)

            step(
                "Location services enabled",
                "After successfully enabling location services, the app navigates back to " +
                    "the home screen."
            )

            await.atMost(10, SECONDS) ignoreException NoMatchingViewException::class untilAsserted {
                statusRobot.checkActivityIsDisplayed()
            }
        }
}
