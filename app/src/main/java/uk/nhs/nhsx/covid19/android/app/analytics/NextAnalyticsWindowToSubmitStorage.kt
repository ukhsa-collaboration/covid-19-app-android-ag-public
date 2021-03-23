package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.Instant
import javax.inject.Inject

class NextAnalyticsWindowToSubmitStorage @Inject constructor(
    private val nextAnalyticsWindowToSubmitJsonStorage: NextAnalyticsWindowToSubmitJsonStorage,
    private val analyticsLogStorage: AnalyticsLogStorage,
    moshi: Moshi
) {
    private val serializationAdapter = moshi.adapter(Instant::class.java)

    var windowStartDate: Instant?
        get() {
            return nextAnalyticsWindowToSubmitJsonStorage.value?.let {
                runCatching {
                    serializationAdapter.fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        oldestLogEntryInstant()
                    } // TODO add crash analytics and come up with a more sophisticated solution
            } ?: oldestLogEntryInstant()
        }
        set(value) {
            nextAnalyticsWindowToSubmitJsonStorage.value = serializationAdapter.toJson(value)
        }

    private fun oldestLogEntryInstant() = analyticsLogStorage.value.map { it.instant }.minOrNull()
}

class NextAnalyticsWindowToSubmitJsonStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "NEXT_ANALYTICS_WINDOW_TO_SUBMIT"
    }
}
