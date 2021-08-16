package uk.nhs.nhsx.covid19.android.app.availability

import android.content.SharedPreferences
import android.os.Build
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAvailabilityProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {
    var appAvailability: AppAvailabilityResponse? by storage(APP_AVAILABILITY_RESPONSE)

    fun isAppAvailable(): Boolean {
        val appAvailability = appAvailability
        return if (appAvailability == null) {
            true
        } else {
            BuildConfig.VERSION_CODE >= appAvailability.minimumAppVersion.value &&
                Build.VERSION.SDK_INT >= appAvailability.minimumSdkVersion.value
        }
    }

    fun isUpdateRecommended() =
        appAvailability?.let {
            BuildConfig.VERSION_CODE < it.recommendedAppVersion.value
        } ?: false

    companion object {
        const val APP_AVAILABILITY_RESPONSE = "APP_AVAILABILITY_RESPONSE"
    }
}
