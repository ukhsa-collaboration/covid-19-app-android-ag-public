package uk.nhs.nhsx.covid19.android.app.analytics

import android.os.Build
import android.os.Build.VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.BuildConfig
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

            Timber.d("aggregating analytics")

            val analyticsPayload = AnalyticsPayload(
                analyticsWindow = getAnalyticsWindow(),
                metrics = updateMetrics(),
                metadata = getMetadata(),
                includesMultipleApplicationVersions = updateStatusStorage.value.defaultFalse()
            )

            analyticsMetricsStorage.reset()

            val newEvents = analyticsEventsStorage.value.orEmpty().plus(analyticsPayload)

            analyticsEventsStorage.value = newEvents
        }
    }

    private fun getAnalyticsWindow(): AnalyticsWindow {
        val window = getAnalyticsWindow.invoke()

        return AnalyticsWindow(
            startDate = window.first.toISOSecondsFormat(),
            endDate = window.second.toISOSecondsFormat()
        )
    }

    private fun updateMetrics(): Metrics =
        analyticsMetricsStorage.metrics.apply {
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
