package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import java.time.Instant
import javax.inject.Inject

class NextAnalyticsWindowToSubmitStorage @Inject constructor(
    private val getOldestLogEntryInstant: GetOldestLogEntryInstant,
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private var storedWindowStartDate: Instant? by storage(VALUE_KEY)
    var windowStartDate: Instant?
        get() = storedWindowStartDate ?: getOldestLogEntryInstant()
        set(value) {
            storedWindowStartDate = value
        }

    companion object {
        const val VALUE_KEY = "NEXT_ANALYTICS_WINDOW_TO_SUBMIT"
    }
}

class GetOldestLogEntryInstant @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage
) {
    operator fun invoke() = analyticsLogStorage.value.map { it.instant }.minOrNull()
}
