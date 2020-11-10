package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.RecommendedAppVersion

class MockAppAvailabilityApi : AppAvailabilityApi {
    override suspend fun getAvailability(): AppAvailabilityResponse =
        AppAvailabilityResponse(
            minimumAppVersion = MinimumAppVersion(
                description = Translatable(mapOf("en-GB" to "Please Update")),
                value = 18
            ),
            minimumSdkVersion = MinimumSdkVersion(
                description = Translatable(mapOf("en-GB" to "Not supported")),
                value = 23
            ),
            recommendedAppVersion = RecommendedAppVersion(
                description = Translatable(mapOf("en-GB" to "There is a newer version of this app available. Please click Update now to proceed to the app store for the update. If you are unable to start the process at this time, you may choose to postpone.")),
                value = 18,
                title = Translatable(mapOf("en-GB" to "Update your NHS Test and Trace app to the latest version"))
            )
        )
}
