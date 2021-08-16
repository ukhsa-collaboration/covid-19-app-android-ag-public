package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject

class IsolationConfigurationProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    var durationDays: DurationDays by storage(DURATION_DAYS_KEY, default = DurationDays())

    companion object {
        const val DURATION_DAYS_KEY = "DURATION_DAYS_KEY"
    }
}
