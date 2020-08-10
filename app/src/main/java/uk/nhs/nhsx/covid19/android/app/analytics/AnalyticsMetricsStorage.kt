package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class AnalyticsMetricsStorage @Inject constructor(
    analyticsMetricsJsonStorage: AnalyticsMetricsJsonStorage,
    moshi: Moshi
) {
    fun reset() {
        val updatedMetrics = metrics.apply {
            canceledCheckIn = 0
            checkedIn = 0
            completedOnboarding = 0
            completedQuestionnaireAndStartedIsolation = 0
            completedQuestionnaireButDidNotStartIsolation = 0
            encounterDetectionPausedBackgroundTick = 0
            hasHadRiskyContactBackgroundTick = 0
            hasSelfDiagnosedPositiveBackgroundTick = 0
            isIsolatingBackgroundTick = 0
            receivedNegativeTestResult = 0
            receivedPositiveTestResult = 0
            receivedVoidTestResult = 0
            runningNormallyBackgroundTick = 0
            totalBackgroundTasks = 0
        }
        metrics = updatedMetrics
    }

    private val adapter = moshi.adapter(Metrics::class.java)

    private var metricsJson = analyticsMetricsJsonStorage.metricsJson

    var metrics: Metrics
        get() = metricsJson?.let {
            adapter.fromJson(it) ?: Metrics()
        } ?: Metrics()
        set(value) {
            metricsJson = adapter.toJson(value)
        }
}

class AnalyticsMetricsJsonStorage @Inject constructor(sharedPreferences: SharedPreferences) {

    private val metricsPrefs = sharedPreferences.with<String>(ANALYTICS_METRICS_KEY)

    var metricsJson by metricsPrefs

    companion object {
        const val ANALYTICS_METRICS_KEY = "ANALYTICS_METRICS_KEY"
    }
}
