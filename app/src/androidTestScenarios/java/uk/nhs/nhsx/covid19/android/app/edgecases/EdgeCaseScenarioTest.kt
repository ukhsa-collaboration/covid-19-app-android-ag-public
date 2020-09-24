package uk.nhs.nhsx.covid19.android.app.edgecases

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.EnableBluetoothRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.EnableLocationRobot

class EdgeCaseScenarioTest : EspressoTest() {

    private val enableBluetoothRobot = EnableBluetoothRobot()
    private val enableLocationRobot = EnableLocationRobot()
    private val statusRobot = StatusRobot()

    @Test
    fun openingHomeActivityWithBluetoothDisabled_shouldNavigateToEnableBluetoothActivity() =
        notReported {
            startTestActivity<StatusActivity>()

            testAppContext.setBluetoothEnabled(false)

            waitFor { enableBluetoothRobot.checkActivityIsDisplayed() }
        }

    @Test
    fun openingHomeActivityWithBluetoothDisabledThenEnableBluetooth_shouldNavigateBackToHomeActivity() =
        reporter(
            "Edge case",
            "Enable bluetooth from app",
            "When the user opens the app with Bluetooth disabled, a screen is shown that urges them to start Bluetooth.",
            Reporter.Kind.FLOW
        ) {
            startTestActivity<StatusActivity>()

            testAppContext.setBluetoothEnabled(false)

            waitFor { enableBluetoothRobot.checkActivityIsDisplayed() }

            step(
                "Start",
                "The user is presented with a screen that shows that bluetooth has to be enabled to use the app."
            )

            testAppContext.setBluetoothEnabled(true)

            step(
                "Bluetooth enabled",
                "After successfully enabling bluetooth, the app navigates back to the home screen."
            )

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }

    @Test
    fun openingHomeActivityWithLocationServicesDisabled_shouldNavigateToEnableLocationActivity() =
        notReported {
            startTestActivity<StatusActivity>()

            testAppContext.setLocationEnabled(false)

            waitFor { enableLocationRobot.checkActivityIsDisplayed() }
        }

    @Test
    fun openingHomeActivityWithLocationServicesDisabledThenEnableLocationServices_shouldNavigateBackToHomeActivity() =
        reporter(
            "Edge case",
            "Enable location services from app",
            "When the user opens the app with location services disabled, a screen is shown that urges them to start location services.",
            Reporter.Kind.FLOW
        ) {
            startTestActivity<StatusActivity>()

            testAppContext.setLocationEnabled(false)

            waitFor { enableLocationRobot.checkActivityIsDisplayed() }

            step(
                "Start",
                "The user is presented with a screen that shows that location services has to be enabled to use the app."
            )

            testAppContext.setLocationEnabled(true)

            step(
                "Location services enabled",
                "After successfully enabling location services, the app navigates back to the home screen."
            )

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
}
