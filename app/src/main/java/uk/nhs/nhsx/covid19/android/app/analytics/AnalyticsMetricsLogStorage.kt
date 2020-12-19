package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Instant
import javax.inject.Inject

@Deprecated(message = "That class is deprecated and will be removed in the future")
class AnalyticsMetricsLogStorage @Inject constructor(
    private val analyticsMetricsLogJsonStorage: AnalyticsMetricsLogJsonStorage,
    private val moshi: Moshi
) {

    private val analyticsMetricsLogSerializationAdapter: JsonAdapter<List<MetricsLogEntry>> =
        moshi.adapter(listOfMetricsLogEntriesType)

    private val lock = Object()

    var value: List<MetricsLogEntry>
        get() = synchronized(lock) {
            analyticsMetricsLogJsonStorage.value?.let {
                runCatching {
                    analyticsMetricsLogSerializationAdapter.fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        listOf()
                    } // TODO add crash analytics and come up with a more sophisticated solution
            } ?: listOf()
        }
        set(value) = synchronized(lock) {
            analyticsMetricsLogJsonStorage.value =
                analyticsMetricsLogSerializationAdapter.toJson(value)
        }

    fun add(metricsLogEntry: MetricsLogEntry) = synchronized(lock) {
        val updatedList = value.toMutableList().apply {
            add(metricsLogEntry)
        }
        value = updatedList
    }

    fun remove(startInclusive: Instant, endExclusive: Instant) = synchronized(lock) {
        val updatedList = value.filter {
            it.instant.isBefore(startInclusive) ||
                it.instant.isAfter(endExclusive) ||
                it.instant == endExclusive
        }
        value = updatedList
    }

    companion object {
        val listOfMetricsLogEntriesType: Type = Types.newParameterizedType(
            List::class.java,
            MetricsLogEntry::class.java
        )
    }
}

class AnalyticsMetricsLogJsonStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "ANALYTICS_METRICS_LOG_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class MetricsLogEntry(
    val metrics: Metrics,
    val instant: Instant
)
