package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class IsolationConfigurationProvider @Inject constructor(
    private val durationJson: IsolationConfigurationJsonProvider,
    moshi: Moshi
) {

    private val durationDaysAdapter =
        moshi.adapter(DurationDays::class.java)

    var durationDays: DurationDays
        get() =
            runCatching {
                durationJson.durationJson?.let {
                    durationDaysAdapter.fromJson(it)
                } ?: DurationDays()
            }.getOrElse { DurationDays() }
        set(value) {
            durationJson.durationJson = durationDaysAdapter.toJson(value)
        }
}

class IsolationConfigurationJsonProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private var durationDaysPref = sharedPreferences.with<String>(DURATION_DAYS_KEY)

    var durationJson by durationDaysPref

    companion object {
        const val DURATION_DAYS_KEY = "DURATION_DAYS_KEY"
    }
}
