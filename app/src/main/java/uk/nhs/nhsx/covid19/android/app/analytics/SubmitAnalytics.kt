package uk.nhs.nhsx.covid19.android.app.analytics

import android.os.Build
import android.os.Build.VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Instant
import javax.inject.Inject

class SubmitAnalytics @Inject constructor(
    private val analyticsMetricsStorage: AnalyticsMetricsStorage,
    private val postCodeProvider: PostCodeProvider,
    private val networkTrafficStats: NetworkTrafficStats,
    private val updateStatusStorage: UpdateStatusStorage,
    private val analyticsLastSubmittedDate: AnalyticsLastSubmittedDate,
    private val analyticsApi: AnalyticsApi
) {

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        runSafely {

            val currentTime = Instant.now().toISOSecondsFormat()

            val lastSubmittedDate = analyticsLastSubmittedDate.lastSubmittedDate
            if (lastSubmittedDate == null) {
                analyticsLastSubmittedDate.lastSubmittedDate = currentTime
                return@runSafely
            }

            val analyticsPayload = AnalyticsPayload(
                analyticsWindow = getAnalyticsWindow(lastSubmittedDate, currentTime),
                metrics = updateMetrics(),
                metadata = getMetadata(),
                includesMultipleApplicationVersions = updateStatusStorage.value.defaultFalse()
            )

            analyticsMetricsStorage.reset()
            analyticsLastSubmittedDate.lastSubmittedDate = currentTime

            analyticsApi.submitAnalytics(analyticsPayload)
        }
    }

    private fun getAnalyticsWindow(startDate: String, endDate: String) = AnalyticsWindow(
        startDate = startDate,
        endDate = endDate
    )

    private fun updateMetrics(): Metrics =
        analyticsMetricsStorage.metrics.apply {
            cumulativeDownloadBytes = networkTrafficStats.getTotalBytesDownloaded()
            cumulativeUploadBytes = networkTrafficStats.getTotalBytesUploaded()
        }.also {
            analyticsMetricsStorage.metrics = it
        }

    private fun getMetadata(): Metadata {
        return Metadata(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            latestApplicationVersion = BuildConfig.VERSION_CODE.toString(),
            postalDistrict = postCodeProvider.value.orEmpty(),
            operatingSystemVersion = "${VERSION.SDK_INT}"
        )
    }
}
