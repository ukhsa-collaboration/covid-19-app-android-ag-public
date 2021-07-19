package uk.nhs.nhsx.covid19.android.app.onboarding

import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.setExposureNotificationResolutionRequired
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EnableExposureNotificationsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PermissionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.DeviceNotSupportedRobot

class PermissionActivityTest : EspressoTest() {

    private val permissionRobot = PermissionRobot()
    private val statusRobot = StatusRobot()
    private val enableExposureNotificationsRobot = EnableExposureNotificationsRobot()
    private val deviceNotSupportedRobot = DeviceNotSupportedRobot()

    @Before
    fun setUp() {
        testAppContext.getExposureNotificationApi().setEnabled(false)
    }

    @After
    fun tearDown() {
        testAppContext.getExposureNotificationApi().setEnabled(true)
        testAppContext.getExposureNotificationApi().activationResult = Success()
    }

    @Test
    fun grantExposureNotificationPermissions_whenError_shouldShowDeviceNotSupportedScreen() {
        testAppContext.getExposureNotificationApi().activationResult = Result.Error()

        startTestActivity<PermissionActivity>()

        permissionRobot.checkActivityIsDisplayed()

        permissionRobot.clickEnablePermissions()

        waitFor { deviceNotSupportedRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun grantExposureNotificationPermissions_whenSuccessful_shouldShowStatusScreen() {
        testAppContext.getExposureNotificationApi().activationResult = Success()

        startTestActivity<PermissionActivity>()

        permissionRobot.checkActivityIsDisplayed()

        permissionRobot.clickEnablePermissions()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun grantExposureNotificationPermissions_whenResolutionNeededAndSuccessful_shouldShowStatusScreen() {
        val activity = startTestActivity<PermissionActivity>()

        testAppContext.setExposureNotificationResolutionRequired(activity!!, true)

        waitFor { permissionRobot.checkActivityIsDisplayed() }

        permissionRobot.clickEnablePermissions()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun grantExposureNotificationPermissions_whenResolutionNeededAndNotSuccessful_shouldEnableExposureNotificationsScreen_whenSuccessful_shouldShowStatusScreen() {
        val activity = startTestActivity<PermissionActivity>()

        testAppContext.setExposureNotificationResolutionRequired(activity!!, false)

        waitFor { permissionRobot.checkActivityIsDisplayed() }

        permissionRobot.clickEnablePermissions()

        waitFor { enableExposureNotificationsRobot.checkActivityIsDisplayed() }

        testAppContext.getExposureNotificationApi().activationResult = Success()

        enableExposureNotificationsRobot.clickEnableExposureNotificationsButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun grantExposureNotificationPermissions_whenResolutionNeededAndNotSuccessful_shouldEnableExposureNotificationsScreen_whenBackPressed_shouldStayOnPermissionScreen() {
        val activity = startTestActivity<PermissionActivity>()

        testAppContext.setExposureNotificationResolutionRequired(activity!!, false)

        waitFor { permissionRobot.checkActivityIsDisplayed() }

        permissionRobot.clickEnablePermissions()

        waitFor { enableExposureNotificationsRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { permissionRobot.checkActivityIsDisplayed() }
    }
}
