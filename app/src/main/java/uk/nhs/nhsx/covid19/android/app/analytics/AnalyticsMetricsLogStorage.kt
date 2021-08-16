package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.listStorage
import java.time.Instant
import javax.inject.Inject

@Deprecated(message = "That class is deprecated and will be removed in the future")
class AnalyticsMetricsLogStorage @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    var value: List<MetricsLogEntry> by listStorage(ANALYTICS_METRICS_LOG_KEY, default = emptyList())

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
        const val ANALYTICS_METRICS_LOG_KEY = "ANALYTICS_METRICS_LOG_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class MetricsLogEntry(
    val metrics: Metrics,
    val instant: Instant
)
