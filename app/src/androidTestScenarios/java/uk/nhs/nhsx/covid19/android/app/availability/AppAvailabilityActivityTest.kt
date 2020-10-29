package uk.nhs.nhsx.covid19.android.app.availability

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.Available
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AppAvailabilityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class AppAvailabilityActivityTest : EspressoTest() {

    private val appAvailabilityActivityRobot = AppAvailabilityRobot()

    private val statusRobot = StatusRobot()

    @Test
    fun showDeviceSdkIsNotSupported() = notReported {

        testAppContext.setAppAvailability(deviceSdkIsNotSupported)

        startTestActivity<AppAvailabilityActivity>()

        appAvailabilityActivityRobot.checkActivityDisplaysUpdateOS()
        appAvailabilityActivityRobot.checkActivityDisplaysMessage(deviceSdkIsNotSupportedMessage)
        appAvailabilityActivityRobot.checkActivityGoToPlayStoreNotDisplayed()
    }

    @Test
    fun showAppVersionIsNotSupported() = notReported {
        testAppContext.setAppAvailability(appVersionNotSupported)

        startTestActivity<AppAvailabilityActivity>()

        appAvailabilityActivityRobot.checkActivityDisplaysCantRunApp()
        appAvailabilityActivityRobot.checkActivityDisplaysMessage(appVersionNotSupportedMessage)
        appAvailabilityActivityRobot.checkActivityGoToPlayStoreNotDisplayed()
    }

    @Test
    fun showUpdateAvailable() = notReported {
        testAppContext.setAppAvailability(appVersionNotSupported)
        testAppContext.updateManager.availableUpdateStatus =
            Available(Int.MAX_VALUE)

        startTestActivity<AppAvailabilityActivity>()

        appAvailabilityActivityRobot.checkActivityDisplaysUpdateApp()
        appAvailabilityActivityRobot.checkActivityDisplaysMessage(appVersionNotSupportedMessage)
        appAvailabilityActivityRobot.checkActivityGoToPlayStoreDisplayed()
    }

    @Test
    fun showStatusScreenWhenPassingAllAppAvailabilityChecks() = notReported {
        testAppContext.setAppAvailability(supported)

        startTestActivity<AppAvailabilityActivity>()

        statusRobot.checkActivityIsDisplayed()
    }

    private val deviceSdkIsNotSupportedMessage = "deviceSdkIsNotSupported, minimumSdkVersion"

    private val appVersionNotSupportedMessage = "appVersionNotSupported, minimumAppVersion"

    private val deviceSdkIsNotSupported = AppAvailabilityResponse(
        minimumAppVersion = MinimumAppVersion(
            Translatable(
                emptyMap()
            ),
            0
        ),
        minimumSdkVersion = MinimumSdkVersion(
            Translatable(
                mapOf(
                    "en" to deviceSdkIsNotSupportedMessage
                )
            ),
            Int.MAX_VALUE
        )
    )

    private val appVersionNotSupported = AppAvailabilityResponse(
        minimumAppVersion = MinimumAppVersion(
            Translatable(
                mapOf(
                    "en" to appVersionNotSupportedMessage
                )
            ),
            Int.MAX_VALUE
        ),
        minimumSdkVersion = MinimumSdkVersion(
            Translatable(
                emptyMap()
            ),
            0
        )
    )

    private val supported = AppAvailabilityResponse(
        minimumAppVersion = MinimumAppVersion(
            Translatable(
                emptyMap()
            ),
            0
        ),
        minimumSdkVersion = MinimumSdkVersion(
            Translatable(
                emptyMap()
            ),
            0
        )
    )
}
