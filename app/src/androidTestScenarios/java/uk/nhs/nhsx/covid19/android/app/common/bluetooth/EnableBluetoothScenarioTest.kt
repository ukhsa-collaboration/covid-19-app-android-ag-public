package uk.nhs.nhsx.covid19.android.app.common.bluetooth

import androidx.test.espresso.Espresso.pressBack
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ContactTracingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.EnableBluetoothRobot

@RunWith(Parameterized::class)
class EnableBluetoothScenarioTest(override val configuration: TestConfiguration) : EspressoTest() {

    private val enableBluetoothRobot = EnableBluetoothRobot()
    private val statusRobot = StatusRobot()
    private val contactTracingHubRobot = ContactTracingHubRobot()
    private val splashScreenRobot = EnableBluetoothRobot()
    private val isolationHubRobot = IsolationHubRobot()

    @After
    fun tearDown() {
        testAppContext.packageManager.clear()
    }

    @Test
    fun openingHomeActivityWithBluetoothDisabled_shouldNavigateToEnableBluetoothActivity_thenContinueWithoutBluetooth_shouldNavigateBackToHomeActivity() {
        testAppContext.setBluetoothEnabled(false)

        startTestActivity<StatusActivity>()

        with(enableBluetoothRobot) {
            checkActivityIsDisplayed()
            clickContinueWithoutBluetooth()
        }
        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun openingHomeActivityWithBluetoothDisabled_shouldNavigateToEnableBluetoothActivity_thenPressBackButton_shouldNavigateBackToHomeActivity() {
        testAppContext.setBluetoothEnabled(false)

        startTestActivity<StatusActivity>()

        with(enableBluetoothRobot) {
            checkActivityIsDisplayed()
            pressBack()
        }
        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    @Reported
    fun openingHomeActivityWithBluetoothDisabledThenEnableBluetooth_thenUserEnablesBluetooth_shouldNavigateBackToHomeActivity() =
        reporter(
            "Edge cases",
            "Enable bluetooth from app",
            "When the user opens the app with Bluetooth disabled, a screen is shown that urges them to start Bluetooth.",
            Reporter.Kind.FLOW
        ) {
            testAppContext.setBluetoothEnabled(false)

            startTestActivity<StatusActivity>()

            enableBluetoothRobot.checkActivityIsDisplayed()

            step(
                "Start",
                "The user is presented with a screen that shows that bluetooth has to be enabled to use the app."
            )

            testAppContext.setBluetoothEnabled(true)

            step(
                "Bluetooth enabled",
                "After successfully enabling bluetooth, the app navigates back to the home screen."
            )

            statusRobot.checkActivityIsDisplayed()
        }

    @Test
    fun bluetoothScreenIsShownOnlyOnce() {
        testAppContext.setBluetoothEnabled(false)

        startTestActivity<StatusActivity>()

        enableBluetoothRobot.checkActivityIsDisplayed()
        testAppContext.setBluetoothEnabled(true)

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickManageContactTracing()
        contactTracingHubRobot.checkActivityIsDisplayed()
        pressBack()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun openingHomeActivityWithBluetoothDisabledAndPendingIsolationHubNavigation_shouldNavigateToContactTracingHub_thenNavigateToBluetoothScreen() {
        testAppContext.setBluetoothEnabled(false)

        startTestActivity<StatusActivity> {
            putExtra(StatusActivity.STATUS_ACTIVITY_ACTION, StatusActivityAction.NavigateToIsolationHub)
        }

        isolationHubRobot.checkActivityIsDisplayed()

        pressBack()

        enableBluetoothRobot.checkActivityIsDisplayed()
    }

    @Test
    fun openingHomeActivityWithBluetoothDisabledButUserHasAlreadySeenBluetoothActivity_shouldStayOnHomeActivity() {
        testAppContext.setBluetoothEnabled(true)
        testAppContext.getShouldShowBluetoothSplashScreen().setHasBeenShown(true)

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenBluetoothDisabled_clicksOnManageContactTracing_shouldNavigateToBluetoothSplashScreen() {
        startTestActivity<StatusActivity>()
        testAppContext.setBluetoothEnabled(false)

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickManageContactTracing()

        splashScreenRobot.checkActivityIsDisplayed()

        testAppContext.setBluetoothEnabled(true)
        contactTracingHubRobot.checkActivityIsDisplayed()
    }

    @Test
    fun givenOnManageContactTracing_whenBluetoothTurnedOff_shouldNavigateToBluetoothSplashScreen() {
        testAppContext.setBluetoothEnabled(true)
        startTestActivity<StatusActivity>()
        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickManageContactTracing()

        contactTracingHubRobot.checkActivityIsDisplayed()
        testAppContext.setBluetoothEnabled(false)

        splashScreenRobot.checkActivityIsDisplayed()
    }
}
