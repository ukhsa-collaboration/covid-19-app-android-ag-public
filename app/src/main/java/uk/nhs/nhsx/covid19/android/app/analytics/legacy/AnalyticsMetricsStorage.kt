package uk.nhs.nhsx.covid19.android.app.analytics.legacy

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

@Deprecated("please use AnalyticsMetricsLogStorage")
class AnalyticsMetricsStorage @Inject constructor(
    private val analyticsMetricsJsonStorage: AnalyticsMetricsJsonStorage,
    moshi: Moshi
) {

    private val adapter = moshi.adapter(Metrics::class.java)

    var metrics: Metrics?
        get() = analyticsMetricsJsonStorage.value?.let {
            kotlin.runCatching {
                adapter.fromJson(it) ?: Metrics()
            }.getOrNull()
        }
        set(value) {
            if (value == null) {
                analyticsMetricsJsonStorage.value = null
            } else {
                analyticsMetricsJsonStorage.value = adapter.toJson(value)
            }
        }
}

@Deprecated("please use AnalyticsMetricsLogStorage")
class AnalyticsMetricsJsonStorage @Inject constructor(sharedPreferences: SharedPreferences) {

    private val metricsPrefs = sharedPreferences.with<String>(ANALYTICS_METRICS_KEY)

    var value by metricsPrefs

    companion object {
        const val ANALYTICS_METRICS_KEY = "ANALYTICS_METRICS_KEY"
    }
}
