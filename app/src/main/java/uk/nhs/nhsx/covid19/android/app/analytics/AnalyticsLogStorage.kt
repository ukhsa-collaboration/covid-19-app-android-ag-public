package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Instant
import javax.inject.Inject

class AnalyticsLogStorage @Inject constructor(
    private val analyticsLogEntryJsonStorage: AnalyticsLogEntryJsonStorage,
    moshi: Moshi
) {

    private val analyticsLogEntriesSerializationAdapter: JsonAdapter<List<AnalyticsLogEntry>> =
        moshi.adapter(listOfAnalyticsLogEntriesType)

    private val lock = Object()

    var value: List<AnalyticsLogEntry>
        get() = synchronized(lock) {
            analyticsLogEntryJsonStorage.value?.let {
                runCatching {
                    analyticsLogEntriesSerializationAdapter.fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        listOf()
                    } // TODO add crash analytics and come up with a more sophisticated solution
            } ?: listOf()
        }
        set(value) = synchronized(lock) {
            analyticsLogEntryJsonStorage.value =
                analyticsLogEntriesSerializationAdapter.toJson(value)
        }

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

    companion object {
        private val listOfAnalyticsLogEntriesType: Type = Types.newParameterizedType(
            List::class.java,
            AnalyticsLogEntry::class.java
        )

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

class AnalyticsLogEntryJsonStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "ANALYTICS_EVENTS_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class AnalyticsLogEntry(
    val instant: Instant,
    val logItem: AnalyticsLogItem
)
