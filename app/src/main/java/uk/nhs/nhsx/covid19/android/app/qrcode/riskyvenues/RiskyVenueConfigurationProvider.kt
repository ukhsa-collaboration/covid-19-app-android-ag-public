package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class RiskyVenueConfigurationProvider @Inject constructor(
    private val durationStorage: RiskyVenueConfigurationStorage,
    moshi: Moshi
) {

    private val durationDaysAdapter =
        moshi.adapter(RiskyVenueConfigurationDurationDays::class.java)

    var durationDays: RiskyVenueConfigurationDurationDays
        get() =
            runCatching {
                durationStorage.value?.let {
                    durationDaysAdapter.fromJson(it)
                } ?: RiskyVenueConfigurationDurationDays()
            }.getOrElse { RiskyVenueConfigurationDurationDays() }
        set(value) {
            durationStorage.value = durationDaysAdapter.toJson(value)
        }
}

class RiskyVenueConfigurationStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private var prefs = sharedPreferences.with<String>(RISKY_VENUE_CONFIGURATION_DURATION_DAYS_KEY)

    var value: String? by prefs

    companion object {
        const val RISKY_VENUE_CONFIGURATION_DURATION_DAYS_KEY = "RISKY_VENUE_CONFIGURATION_DURATION_DAYS_KEY"
    }
}
