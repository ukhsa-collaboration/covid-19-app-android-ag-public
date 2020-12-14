package uk.nhs.nhsx.covid19.android.app.common

import android.app.Activity
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Error
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EnableExposureNotificationsRobot

class EnableExposureNotificationsActivityTest : EspressoTest() {

    @get:Rule
    val activityRule = ActivityTestRule<EnableExposureNotificationsActivity>(EnableExposureNotificationsActivity::class.java)

    private val enableExposureNotificationsRobot = EnableExposureNotificationsRobot()

    @Before
    fun setUp() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
    }

    @Test
    fun showScreen() = notReported {
        enableExposureNotificationsRobot.checkActivityIsDisplayed()
    }

    @Test
    fun grantExposureNotificationPermissions_whenError_shouldShowError() = notReported {
        testAppContext.getExposureNotificationApi().activationResult = Error()

        enableExposureNotificationsRobot.checkActivityIsDisplayed()

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()

        waitFor { enableExposureNotificationsRobot.checkErrorIsDisplayed() }
    }

    @Test
    fun grantExposureNotificationPermissions_whenSuccessful_shouldFinishWithOk() = notReported {
        testAppContext.getExposureNotificationApi().activationResult = Success()

        enableExposureNotificationsRobot.checkActivityIsDisplayed()

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()

        waitFor { assertThat(activityRule.activityResult, hasResultCode(Activity.RESULT_OK)) }
    }

    @Test
    fun grantExposureNotificationPermissions_whenResolutionNeededAndSuccessful_shouldFinishWithOk() = notReported {
        testAppContext.setExposureNotificationResolutionRequired(activityRule.activity, true)

        waitFor { enableExposureNotificationsRobot.checkActivityIsDisplayed() }

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()

        waitFor { assertThat(activityRule.activityResult, hasResultCode(Activity.RESULT_OK)) }
    }

    @Test
    fun grantExposureNotificationPermissions_whenResolutionNeededAndNotSuccessful_shouldDoNothing() = notReported {
        testAppContext.setExposureNotificationResolutionRequired(activityRule.activity, false)

        waitFor { enableExposureNotificationsRobot.checkActivityIsDisplayed() }

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()
    }
}
