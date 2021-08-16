package uk.nhs.nhsx.covid19.android.app.analytics.legacy

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject

@Deprecated("please use AnalyticsMetricsLogStorage")
class AnalyticsMetricsStorage @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    var metrics: Metrics? by storage(ANALYTICS_METRICS_KEY)

    companion object {
        const val ANALYTICS_METRICS_KEY = "ANALYTICS_METRICS_KEY"
    }
}
