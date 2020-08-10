package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.TranslatedString
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumSdkVersion

class MockAppAvailabilityApi : AppAvailabilityApi {
    override suspend fun getAvailability(): AppAvailabilityResponse =
        AppAvailabilityResponse(
            minimumAppVersion = MinimumAppVersion(
                TranslatedString(enGB = "Please Update"),
                value = 18
            ),
            minimumSdkVersion = MinimumSdkVersion(
                TranslatedString(enGB = "Not supported"),
                value = 23
            )
        )
}
