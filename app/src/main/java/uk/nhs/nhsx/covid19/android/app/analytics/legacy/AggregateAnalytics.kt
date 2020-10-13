package uk.nhs.nhsx.covid19.android.app.analytics.legacy

import android.os.Build
import android.os.Build.VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.analytics.GetAnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.analytics.NetworkTrafficStats
import uk.nhs.nhsx.covid19.android.app.analytics.UpdateStatusStorage
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import javax.inject.Inject

@Deprecated("Use GroupAnalyticsEvents, this is only for migration")
class AggregateAnalytics @Inject constructor(
    private val analyticsMetricsStorage: AnalyticsMetricsStorage,
    private val postCodeProvider: PostCodeProvider,
    private val networkTrafficStats: NetworkTrafficStats,
    private val updateStatusStorage: UpdateStatusStorage,
    private val analyticsEventsStorage: AnalyticsEventsStorage,
    private val getAnalyticsWindow: GetAnalyticsWindow
) {

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        runSafely {
            analyticsMetricsStorage.metrics?.let {
                Timber.d("aggregating analytics")

                val analyticsPayload = AnalyticsPayload(
                    analyticsWindow = getAnalyticsWindow(),
                    metrics = updateMetrics(it),
                    metadata = getMetadata(),
                    includesMultipleApplicationVersions = updateStatusStorage.value.defaultFalse()
                )

                analyticsMetricsStorage.metrics = null

                val newEvents = analyticsEventsStorage.value.orEmpty().plus(analyticsPayload)

                analyticsEventsStorage.value = newEvents
            }
            Unit
        }
    }

    private fun getAnalyticsWindow(): AnalyticsWindow {
        val window = getAnalyticsWindow.getLastWindow()

        return AnalyticsWindow(
            startDate = window.first.toISOSecondsFormat(),
            endDate = window.second.toISOSecondsFormat()
        )
    }

    private fun updateMetrics(metrics: Metrics): Metrics =
        metrics.apply {
            cumulativeDownloadBytes = networkTrafficStats.getTotalBytesDownloaded()
            cumulativeUploadBytes = networkTrafficStats.getTotalBytesUploaded()
        }.also {
            analyticsMetricsStorage.metrics = it
        }

    private fun getMetadata(): Metadata {
        val latestApplicationVersion = if (BuildConfig.VERSION_NAME.contains(" ")) {
            BuildConfig.VERSION_NAME.split(" ")[0]
        } else {
            BuildConfig.VERSION_NAME
        }
        return Metadata(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            latestApplicationVersion = latestApplicationVersion,
            postalDistrict = postCodeProvider.value.orEmpty(),
            operatingSystemVersion = "${VERSION.SDK_INT}"
        )
    }
}
