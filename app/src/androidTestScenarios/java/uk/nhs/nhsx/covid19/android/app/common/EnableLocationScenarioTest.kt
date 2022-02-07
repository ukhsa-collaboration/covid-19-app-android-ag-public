package uk.nhs.nhsx.covid19.android.app.common

import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.EnableLocationRobot

@RunWith(Parameterized::class)
class EnableLocationScenarioTest(override val configuration: TestConfiguration) : EspressoTest() {

    private val enableLocationRobot = EnableLocationRobot()
    private val statusRobot = StatusRobot()

    @After
    fun tearDown() {
        testAppContext.packageManager.clear()
    }

    @Test
    fun openingHomeActivityWithLocationServicesDisabled_shouldNavigateToEnableLocationActivity() {
        startTestActivity<StatusActivity>()

        testAppContext.setLocationEnabled(false)

        waitFor { enableLocationRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressingBackInEnableLocationActivity_shouldNotNavigate() {
        startTestActivity<StatusActivity>()

        testAppContext.setLocationEnabled(false)

        waitFor { enableLocationRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        enableLocationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun openingHomeActivityWithLocationServicesDisabled_whenDeviceSupportsLocationlessScanning_shouldStayOnStatusActivity() {
        testAppContext.getExposureNotificationApi().setDeviceSupportsLocationlessScanning(true)
        startTestActivity<StatusActivity>()

        testAppContext.setLocationEnabled(false)

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    @Reported
    fun openingHomeActivityWithLocationServicesDisabledThenEnableLocationServices_shouldNavigateBackToHomeActivity() =
        reporter(
            "Edge cases",
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
