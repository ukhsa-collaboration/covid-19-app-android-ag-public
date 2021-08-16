package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import uk.nhs.nhsx.covid19.android.app.util.listStorage
import java.time.Instant
import javax.inject.Inject

class AnalyticsLogStorage @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    var value: List<AnalyticsLogEntry> by listStorage(VALUE_KEY, default = emptyList())

    fun add(logEntry: AnalyticsLogEntry) = synchronized(lock) {
        val updatedList = value.toMutableList().apply {
            add(logEntry)
        }
        value = updatedList
    }

    fun remove(startInclusive: Instant, endExclusive: Instant) = synchronized(lock) {
        val updatedList = value.filterNot {
            !it.instant.isBefore(startInclusive) && it.instant.isBefore(endExclusive)
        }
        value = updatedList
    }

    fun removeBeforeOrEqual(date: Instant) {
        val updatedList = value.filterNot { it.instant.isBeforeOrEqual(date) }
        value = updatedList
    }

    companion object {
        const val VALUE_KEY = "ANALYTICS_EVENTS_KEY"

        val analyticsLogItemAdapter: PolymorphicJsonAdapterFactory<AnalyticsLogItem> =
            PolymorphicJsonAdapterFactory.of(AnalyticsLogItem::class.java, "type")
                .withSubtype(AnalyticsLogItem.Event::class.java, "Event")
                .withSubtype(
                    AnalyticsLogItem.BackgroundTaskCompletion::class.java,
                    "BackgroundTaskCompletion"
                )
                .withSubtype(AnalyticsLogItem.ResultReceived::class.java, "ResultReceived")
                .withSubtype(AnalyticsLogItem.UpdateNetworkStats::class.java, "UpdateNetworkStats")
                .withSubtype(AnalyticsLogItem.ExposureWindowMatched::class.java, "ExposureWindowMatched")
    }
}

@JsonClass(generateAdapter = true)
data class AnalyticsLogEntry(
    val instant: Instant,
    val logItem: AnalyticsLogItem
)
