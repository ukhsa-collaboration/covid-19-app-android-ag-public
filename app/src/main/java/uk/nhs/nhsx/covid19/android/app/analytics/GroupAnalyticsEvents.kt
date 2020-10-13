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
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.collections.Map.Entry

class GroupAnalyticsEvents(
    private val analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
    private val postCodeProvider: PostCodeProvider,
    private val updateStatusStorage: UpdateStatusStorage,
    private val getAnalyticsWindow: GetAnalyticsWindow,
    private val clock: Clock
) {

    @Inject
    constructor(
        analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
        postCodeProvider: PostCodeProvider,
        updateStatusStorage: UpdateStatusStorage,
        getAnalyticsWindow: GetAnalyticsWindow
    ) : this(
        analyticsMetricsLogStorage,
        postCodeProvider,
        updateStatusStorage,
        getAnalyticsWindow,
        Clock.systemUTC()
    )

    suspend operator fun invoke(shallIncludeCurrentWindow: Boolean = false): Result<List<AnalyticsPayload>> =
        withContext(Dispatchers.IO) {
            runSafely {

                Timber.d("grouping analytics")

                val groupedMetrics: List<Pair<Metrics, AnalyticsWindow>> =
                    analyticsMetricsLogStorage.value
                        .groupBy { (_, instant) -> getAnalyticsWindow(instant) }
                        .filter { entry ->
                            if (entry.key == getAnalyticsWindow(Instant.now(clock)))
                                shallIncludeCurrentWindow
                            else true
                        }
                        .map { entry: Entry<AnalyticsWindow, List<MetricsLogEntry>> ->
                            entry.value.foldRight(Metrics()) { pair, acc -> acc + pair.metrics } to entry.key
                        }

                groupedMetrics.map { (metrics, analyticsWindow) ->
                    AnalyticsPayload(
                        analyticsWindow = analyticsWindow,
                        metrics = metrics,
                        metadata = getMetadata(),
                        includesMultipleApplicationVersions = updateStatusStorage.value.defaultFalse()
                    )
                }
            }
        }

    private fun getAnalyticsWindow(instant: Instant): AnalyticsWindow {
        val window = getAnalyticsWindow.invoke(instant)

        return AnalyticsWindow(
            startDate = window.first.toISOSecondsFormat(),
            endDate = window.second.toISOSecondsFormat()
        )
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

    infix operator fun Metrics.plus(other: Metrics): Metrics =
        this.apply {
            canceledCheckIn += other.canceledCheckIn
            checkedIn += other.checkedIn
            completedOnboarding += other.completedOnboarding
            completedQuestionnaireAndStartedIsolation += other.completedQuestionnaireAndStartedIsolation
            completedQuestionnaireButDidNotStartIsolation += other.completedQuestionnaireButDidNotStartIsolation
            cumulativeDownloadBytes = cumulativeDownloadBytes plus other.cumulativeDownloadBytes
            cumulativeUploadBytes = cumulativeUploadBytes plus other.cumulativeUploadBytes
            encounterDetectionPausedBackgroundTick += other.encounterDetectionPausedBackgroundTick
            hasHadRiskyContactBackgroundTick += other.hasHadRiskyContactBackgroundTick
            hasSelfDiagnosedPositiveBackgroundTick += other.hasSelfDiagnosedPositiveBackgroundTick
            isIsolatingBackgroundTick += other.isIsolatingBackgroundTick
            receivedNegativeTestResult += other.receivedNegativeTestResult
            receivedPositiveTestResult += other.receivedPositiveTestResult
            receivedVoidTestResult += other.receivedVoidTestResult
            runningNormallyBackgroundTick += other.runningNormallyBackgroundTick
            totalBackgroundTasks += other.totalBackgroundTasks
        }

    private infix fun Int?.plus(other: Int?): Int? =
        this?.let { first ->
            other?.let { second -> first + second } ?: first
        } ?: other
}
