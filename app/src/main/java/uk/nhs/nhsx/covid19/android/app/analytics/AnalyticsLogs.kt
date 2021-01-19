package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_ACTIVE_IPC_TOKEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_ISOLATION_PAYMENTS_BUTTON
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.util.toInt

fun List<AnalyticsLogEntry>.toMetrics(): Metrics {
    return Metrics().apply {
        forEach { entry ->
            when (val log = entry.logItem) {
                is Event -> updateRegularEvent(log.eventType)
                is BackgroundTaskCompletion -> updateBackgroundTaskTicks(log.backgroundTaskTicks)
                is ResultReceived -> updateTestResults(log.result, log.testOrderType)
                is UpdateNetworkStats -> updateNetworkStats(log.downloadedBytes, log.uploadedBytes)
            }
        }
    }
}

private fun Metrics.updateRegularEvent(eventType: RegularAnalyticsEventType) {
    when (eventType) {
        QR_CODE_CHECK_IN -> checkedIn++
        CANCELED_CHECK_IN -> canceledCheckIn++
        COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION -> completedQuestionnaireAndStartedIsolation++
        COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION -> completedQuestionnaireButDidNotStartIsolation++
        POSITIVE_RESULT_RECEIVED -> receivedPositiveTestResult++
        NEGATIVE_RESULT_RECEIVED -> receivedNegativeTestResult++
        VOID_RESULT_RECEIVED -> receivedVoidTestResult++
        RECEIVED_RISKY_CONTACT_NOTIFICATION -> receivedRiskyContactNotification = 1
        STARTED_ISOLATION -> startedIsolation++
        RECEIVED_ACTIVE_IPC_TOKEN -> receivedActiveIpcToken++
        SELECTED_ISOLATION_PAYMENTS_BUTTON -> selectedIsolationPaymentsButton++
        LAUNCHED_ISOLATION_PAYMENTS_APPLICATION -> launchedIsolationPaymentsApplication++
    }
}

private fun Metrics.updateTestResults(result: VirologyTestResult, testOrderType: TestOrderType) {
    when {
        result == VOID && testOrderType == INSIDE_APP -> receivedVoidTestResultViaPolling++
        result == VOID && testOrderType == OUTSIDE_APP -> receivedVoidTestResultEnteredManually++
        result == POSITIVE && testOrderType == INSIDE_APP -> receivedPositiveTestResultViaPolling++
        result == POSITIVE && testOrderType == OUTSIDE_APP -> receivedPositiveTestResultEnteredManually++
        result == NEGATIVE && testOrderType == INSIDE_APP -> receivedNegativeTestResultViaPolling++
        result == NEGATIVE && testOrderType == OUTSIDE_APP -> receivedNegativeTestResultEnteredManually++
    }
}

private fun Metrics.updateNetworkStats(downloadedBytes: Int?, uploadedBytes: Int?) {
    cumulativeDownloadBytes = cumulativeDownloadBytes plus downloadedBytes
    cumulativeUploadBytes = cumulativeUploadBytes plus uploadedBytes
}

private fun Metrics.updateBackgroundTaskTicks(backgroundTaskTicks: BackgroundTaskTicks) {
    totalBackgroundTasks++
    runningNormallyBackgroundTick += backgroundTaskTicks.runningNormallyBackgroundTick.toInt()
    isIsolatingBackgroundTick += backgroundTaskTicks.isIsolatingBackgroundTick.toInt()
    isIsolatingForHadRiskyContactBackgroundTick += backgroundTaskTicks.isIsolatingForHadRiskyContactBackgroundTick.toInt()
    hasSelfDiagnosedPositiveBackgroundTick += backgroundTaskTicks.hasSelfDiagnosedPositiveBackgroundTick.toInt()
    isIsolatingForSelfDiagnosedBackgroundTick += backgroundTaskTicks.isIsolatingForSelfDiagnosedBackgroundTick.toInt()
    isIsolatingForTestedPositiveBackgroundTick += backgroundTaskTicks.isIsolatingForTestedPositiveBackgroundTick.toInt()
    hasHadRiskyContactBackgroundTick += backgroundTaskTicks.hasHadRiskyContactBackgroundTick.toInt()
    hasSelfDiagnosedBackgroundTick += backgroundTaskTicks.hasSelfDiagnosedBackgroundTick.toInt()
    hasTestedPositiveBackgroundTick += backgroundTaskTicks.hasTestedPositiveBackgroundTick.toInt()
    encounterDetectionPausedBackgroundTick += backgroundTaskTicks.encounterDetectionPausedBackgroundTick.toInt()
    haveActiveIpcTokenBackgroundTick += backgroundTaskTicks.haveActiveIpcTokenBackgroundTick.toInt()
}

private infix fun Int?.plus(other: Int?): Int? =
    this?.let { first ->
        other?.let { second -> first + second } ?: first
    } ?: other
