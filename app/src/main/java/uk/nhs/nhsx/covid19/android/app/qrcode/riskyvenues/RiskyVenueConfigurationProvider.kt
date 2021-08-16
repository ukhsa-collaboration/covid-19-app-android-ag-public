package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject

class RiskyVenueConfigurationProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    var durationDays: RiskyVenueConfigurationDurationDays by storage(
        key = RISKY_VENUE_CONFIGURATION_DURATION_DAYS_KEY,
        default = RiskyVenueConfigurationDurationDays()
    )

    companion object {
        const val RISKY_VENUE_CONFIGURATION_DURATION_DAYS_KEY = "RISKY_VENUE_CONFIGURATION_DURATION_DAYS_KEY"
    }
}
