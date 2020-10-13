package uk.nhs.nhsx.covid19.android.app.availability

import android.content.SharedPreferences
import android.os.Build
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.remote.data.AppAvailabilityResponse
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAvailabilityProvider @Inject constructor(
    sharedPreferences: SharedPreferences,
    private val moshi: Moshi
) {
    var appAvailability: AppAvailabilityResponse?
        get() = runCatching {
            appAvailabilityStorage?.let {
                moshi.adapter<AppAvailabilityResponse>(AppAvailabilityResponse::class.java).fromJson(it)
            }
        }.getOrNull()
        set(value) {
            appAvailabilityStorage = moshi.adapter<AppAvailabilityResponse>(AppAvailabilityResponse::class.java).toJson(value)
        }

    fun isAppAvailable(): Boolean {
        val appAvailability = appAvailability
        return if (appAvailability == null) {
            true
        } else {
            BuildConfig.VERSION_CODE >= appAvailability.minimumAppVersion.value &&
                Build.VERSION.SDK_INT >= appAvailability.minimumSdkVersion.value
        }
    }

    private val appAvailabilityPrefs = sharedPreferences.with<String>(APP_AVAILABILITY_RESPONSE)
    private var appAvailabilityStorage: String? by appAvailabilityPrefs

    companion object {
        private const val APP_AVAILABILITY_RESPONSE = "APP_AVAILABILITY_RESPONSE"
    }
}
