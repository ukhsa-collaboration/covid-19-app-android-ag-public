@file:Suppress("DEPRECATION")
package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsAlarm
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import java.time.Instant
import javax.inject.Inject

class MigrateMetricsLogStorageToLogStorage @Inject constructor(
    private val analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val analyticsAlarm: AnalyticsAlarm
) {

    operator fun invoke() {
        analyticsAlarm.cancel()

        analyticsMetricsLogStorage.value.forEach { (metrics, instant) ->
            migrateRegularEventMetrics(metrics, instant)
            migrateTestResultMetrics(metrics, instant)
            migrateBackgroundTaskMetrics(metrics, instant)
            migrateNetworkMetrics(metrics, instant)
        }

        analyticsMetricsLogStorage.value = listOf()
    }

    private fun migrateRegularEventMetrics(metrics: Metrics, instant: Instant) {
        if (metrics.checkedIn > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(QR_CODE_CHECK_IN)))
        }
        if (metrics.canceledCheckIn > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(CANCELED_CHECK_IN)))
        }
        if (metrics.completedQuestionnaireAndStartedIsolation > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION)))
        }
        if (metrics.completedQuestionnaireButDidNotStartIsolation > 0) {
            analyticsLogStorage.add(
                AnalyticsLogEntry(instant, Event(COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION))
            )
        }
        if (metrics.receivedPositiveTestResult > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(POSITIVE_RESULT_RECEIVED)))
        }
        if (metrics.receivedNegativeTestResult > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(NEGATIVE_RESULT_RECEIVED)))
        }
        if (metrics.receivedVoidTestResult > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(VOID_RESULT_RECEIVED)))
        }
    }

    private fun migrateTestResultMetrics(metrics: Metrics, instant: Instant) {
        if (metrics.receivedPositiveTestResultEnteredManually > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(POSITIVE, LAB_RESULT, OUTSIDE_APP)))
        }
        if (metrics.receivedPositiveTestResultViaPolling > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(POSITIVE, LAB_RESULT, INSIDE_APP)))
        }
        if (metrics.receivedNegativeTestResultEnteredManually > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(NEGATIVE, LAB_RESULT, OUTSIDE_APP)))
        }
        if (metrics.receivedNegativeTestResultViaPolling > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(NEGATIVE, LAB_RESULT, INSIDE_APP)))
        }
        if (metrics.receivedVoidTestResultEnteredManually > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(VOID, LAB_RESULT, OUTSIDE_APP)))
        }
        if (metrics.receivedVoidTestResultViaPolling > 0) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(VOID, LAB_RESULT, INSIDE_APP)))
        }
    }

    private fun migrateBackgroundTaskMetrics(metrics: Metrics, instant: Instant) {
        if (metrics.totalBackgroundTasks > 0) {
            analyticsLogStorage.add(metrics.getBackgroundTaskTicks(instant))
        }
    }

    private fun migrateNetworkMetrics(metrics: Metrics, instant: Instant) {
        val cumulativeDownloadBytes = metrics.cumulativeDownloadBytes
        val cumulativeUploadBytes = metrics.cumulativeUploadBytes
        if ((cumulativeDownloadBytes != null && cumulativeDownloadBytes > 0) ||
            (cumulativeUploadBytes != null && cumulativeUploadBytes > 0)
        ) {
            analyticsLogStorage.add(metrics.getNetworkStats(instant))
        }
    }

    private fun Metrics.getBackgroundTaskTicks(instant: Instant) = AnalyticsLogEntry(
        instant = instant,
        logItem = BackgroundTaskCompletion(
            BackgroundTaskTicks(
                runningNormallyBackgroundTick = runningNormallyBackgroundTick.toBoolean(),
                isIsolatingBackgroundTick = isIsolatingBackgroundTick.toBoolean(),
                isIsolatingForHadRiskyContactBackgroundTick = isIsolatingForHadRiskyContactBackgroundTick.toBoolean(),
                hasSelfDiagnosedPositiveBackgroundTick = hasSelfDiagnosedPositiveBackgroundTick.toBoolean(),
                isIsolatingForSelfDiagnosedBackgroundTick = isIsolatingForSelfDiagnosedBackgroundTick.toBoolean(),
                isIsolatingForTestedPositiveBackgroundTick = isIsolatingForTestedPositiveBackgroundTick.toBoolean(),
                hasHadRiskyContactBackgroundTick = hasHadRiskyContactBackgroundTick.toBoolean(),
                hasSelfDiagnosedBackgroundTick = hasSelfDiagnosedBackgroundTick.toBoolean(),
                hasTestedPositiveBackgroundTick = hasTestedPositiveBackgroundTick.toBoolean(),
                encounterDetectionPausedBackgroundTick = encounterDetectionPausedBackgroundTick.toBoolean()
            )
        )
    )

    private fun Metrics.getNetworkStats(instant: Instant) = AnalyticsLogEntry(
        instant,
        UpdateNetworkStats(
            downloadedBytes = cumulativeDownloadBytes,
            uploadedBytes = cumulativeUploadBytes
        )
    )

    private fun Int.toBoolean() = this > 0
}
