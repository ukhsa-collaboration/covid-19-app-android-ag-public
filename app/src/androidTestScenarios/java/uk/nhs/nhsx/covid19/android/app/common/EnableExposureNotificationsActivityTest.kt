package uk.nhs.nhsx.covid19.android.app.common

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Error
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EnableExposureNotificationsRobot

class EnableExposureNotificationsActivityTest : EspressoTest() {

    lateinit var scenario: ActivityScenario<EnableExposureNotificationsActivity>

    private val enableExposureNotificationsRobot = EnableExposureNotificationsRobot()

    @Before
    fun setUp() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
        scenario = launch(EnableExposureNotificationsActivity::class.java)
    }

    @Test
    fun showScreen() {
        enableExposureNotificationsRobot.checkActivityIsDisplayed()
    }

    @Test
    fun grantExposureNotificationPermissions_whenError_shouldShowError() {
        testAppContext.getExposureNotificationApi().activationResult = Error()

        enableExposureNotificationsRobot.checkActivityIsDisplayed()

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()

        waitFor { enableExposureNotificationsRobot.checkErrorIsDisplayed() }
    }

    @Test
    fun grantExposureNotificationPermissions_whenSuccessful_shouldFinishWithOk() {
        testAppContext.getExposureNotificationApi().activationResult = Success()

        enableExposureNotificationsRobot.checkActivityIsDisplayed()

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()

        waitFor { assertThat(scenario.result, hasResultCode(Activity.RESULT_OK)) }
    }

    @Test
    fun grantExposureNotificationPermissions_whenResolutionNeededAndSuccessful_shouldFinishWithOk() {
        scenario.onActivity { activity ->
            testAppContext.setExposureNotificationResolutionRequired(activity, true)
        }

        waitFor { enableExposureNotificationsRobot.checkActivityIsDisplayed() }

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()

        waitFor { assertThat(scenario.result, hasResultCode(Activity.RESULT_OK)) }
    }

    @Test
    fun grantExposureNotificationPermissions_whenResolutionNeededAndNotSuccessful_shouldDoNothing() {
        scenario.onActivity { activity ->
            testAppContext.setExposureNotificationResolutionRequired(activity, false)
        }

        waitFor { enableExposureNotificationsRobot.checkActivityIsDisplayed() }

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()
    }
}
