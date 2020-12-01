package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlin.collections.Map.Entry

class GroupAnalyticsEvents(
    private val analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
    private val metadataProvider: MetadataProvider,
    private val updateStatusStorage: UpdateStatusStorage,
    private val getAnalyticsWindow: GetAnalyticsWindow,
    private val clock: Clock
) {

    @Inject
    constructor(
        analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
        metadataProvider: MetadataProvider,
        updateStatusStorage: UpdateStatusStorage,
        getAnalyticsWindow: GetAnalyticsWindow
    ) : this(
        analyticsMetricsLogStorage,
        metadataProvider,
        updateStatusStorage,
        getAnalyticsWindow,
        Clock.systemUTC()
    )

    suspend operator fun invoke(): Result<List<AnalyticsPayload>> =
        withContext(Dispatchers.IO) {
            runSafely {

                Timber.d("grouping analytics")

                val groupedMetrics: List<Pair<Metrics, AnalyticsWindow>> =
                    analyticsMetricsLogStorage.value
                        .groupBy { (_, instant) -> getAnalyticsWindow(instant) }
                        .filterNot { entry -> isToday(entry.key) }
                        .map { entry: Entry<AnalyticsWindow, List<MetricsLogEntry>> ->
                            entry.value.foldRight(Metrics()) { pair, acc -> acc + pair.metrics } to entry.key
                        }

                groupedMetrics.map { (metrics, analyticsWindow) ->
                    AnalyticsPayload(
                        analyticsWindow = analyticsWindow,
                        metrics = metrics,
                        metadata = metadataProvider.getMetadata(),
                        includesMultipleApplicationVersions = updateStatusStorage.value.defaultFalse()
                    )
                }
            }
        }

    private fun isToday(analyticsWindow: AnalyticsWindow): Boolean {
        return analyticsWindow == getAnalyticsWindow(Instant.now(clock))
    }

    private fun getAnalyticsWindow(instant: Instant): AnalyticsWindow {
        val window = getAnalyticsWindow.invoke(instant)

        return AnalyticsWindow(
            startDate = window.first.toISOSecondsFormat(),
            endDate = window.second.toISOSecondsFormat()
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
            receivedVoidTestResultEnteredManually += other.receivedVoidTestResultEnteredManually
            receivedPositiveTestResultEnteredManually += other.receivedPositiveTestResultEnteredManually
            receivedNegativeTestResultEnteredManually += other.receivedNegativeTestResultEnteredManually
            receivedVoidTestResultViaPolling += other.receivedVoidTestResultViaPolling
            receivedPositiveTestResultViaPolling += other.receivedPositiveTestResultViaPolling
            receivedNegativeTestResultViaPolling += other.receivedNegativeTestResultViaPolling
            runningNormallyBackgroundTick += other.runningNormallyBackgroundTick
            totalBackgroundTasks += other.totalBackgroundTasks
            hasSelfDiagnosedBackgroundTick += other.hasSelfDiagnosedBackgroundTick
            hasTestedPositiveBackgroundTick += other.hasTestedPositiveBackgroundTick
            isIsolatingForSelfDiagnosedBackgroundTick += other.isIsolatingForSelfDiagnosedBackgroundTick
            isIsolatingForTestedPositiveBackgroundTick += other.isIsolatingForTestedPositiveBackgroundTick
            isIsolatingForHadRiskyContactBackgroundTick += other.isIsolatingForHadRiskyContactBackgroundTick
        }

    private infix fun Int?.plus(other: Int?): Int? =
        this?.let { first ->
            other?.let { second -> first + second } ?: first
        } ?: other
}
