package uk.nhs.nhsx.covid19.android.app.availability

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.Available
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.RecommendedAppVersion
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AppAvailabilityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class AppAvailabilityActivityTest : EspressoTest() {

    private val appAvailabilityActivityRobot = AppAvailabilityRobot()

    private val statusRobot = StatusRobot()

    @Test
    fun showDeviceSdkIsNotSupported() {

        testAppContext.setAppAvailability(deviceSdkIsNotSupported)

        startTestActivity<AppAvailabilityActivity>()

        appAvailabilityActivityRobot.checkActivityDisplaysUpdateOS()
        appAvailabilityActivityRobot.checkActivityDisplaysMessage(deviceSdkIsNotSupportedMessage)
        appAvailabilityActivityRobot.checkActivityGoToPlayStoreNotDisplayed()
    }

    @Test
    fun showAppVersionIsNotSupported() {
        testAppContext.setAppAvailability(appVersionNotSupported)

        startTestActivity<AppAvailabilityActivity>()

        appAvailabilityActivityRobot.checkActivityDisplaysCantRunApp()
        appAvailabilityActivityRobot.checkActivityDisplaysMessage(appVersionNotSupportedMessage)
        appAvailabilityActivityRobot.checkActivityGoToPlayStoreNotDisplayed()
    }

    @Test
    fun showUpdateAvailable() {
        testAppContext.setAppAvailability(appVersionNotSupported)
        testAppContext.updateManager.availableUpdateStatus =
            Available(Int.MAX_VALUE)

        startTestActivity<AppAvailabilityActivity>()

        appAvailabilityActivityRobot.checkActivityDisplaysUpdateApp()
        appAvailabilityActivityRobot.checkActivityDisplaysMessage(appVersionNotSupportedMessage)
        appAvailabilityActivityRobot.checkActivityGoToPlayStoreDisplayed()
    }

    @Test
    fun showStatusScreenWhenPassingAllAppAvailabilityChecks() {
        testAppContext.setLocalAuthority("1")
        testAppContext.setAppAvailability(supported)

        startTestActivity<AppAvailabilityActivity>()

        statusRobot.checkActivityIsDisplayed()
    }

    private val deviceSdkIsNotSupportedMessage = "deviceSdkIsNotSupported, minimumSdkVersion"

    private val appVersionNotSupportedMessage = "appVersionNotSupported, minimumAppVersion"

    private val deviceSdkIsNotSupported = AppAvailabilityResponse(
        minimumAppVersion = MinimumAppVersion(
            TranslatableString(
                emptyMap()
            ),
            0
        ),
        minimumSdkVersion = MinimumSdkVersion(
            TranslatableString(
                mapOf(
                    "en" to deviceSdkIsNotSupportedMessage
                )
            ),
            Int.MAX_VALUE
        ),
        recommendedAppVersion = RecommendedAppVersion(
            TranslatableString(
                emptyMap()
            ),
            0,
            TranslatableString(
                emptyMap()
            )
        )
    )

    private val appVersionNotSupported = AppAvailabilityResponse(
        minimumAppVersion = MinimumAppVersion(
            TranslatableString(
                mapOf(
                    "en" to appVersionNotSupportedMessage
                )
            ),
            Int.MAX_VALUE
        ),
        minimumSdkVersion = MinimumSdkVersion(
            TranslatableString(
                emptyMap()
            ),
            0
        ),
        recommendedAppVersion = RecommendedAppVersion(
            TranslatableString(
                emptyMap()
            ),
            0,
            title = TranslatableString(
                emptyMap()
            )
        )
    )

    private val supported = AppAvailabilityResponse(
        minimumAppVersion = MinimumAppVersion(
            TranslatableString(
                emptyMap()
            ),
            0
        ),
        minimumSdkVersion = MinimumSdkVersion(
            TranslatableString(
                emptyMap()
            ),
            0
        ),
        recommendedAppVersion = RecommendedAppVersion(
            TranslatableString(
                emptyMap()
            ),
            0,
            title = TranslatableString(
                emptyMap()
            )
        )
    )
}
