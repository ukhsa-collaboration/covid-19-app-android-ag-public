package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ExposureWindowMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID

fun List<AnalyticsLogEntry>.toMetrics(missingSubmissionDays: Int): Metrics {
    return Metrics().apply {
        updateMissingPacketsLast7Days(missingSubmissionDays)
        forEach { entry ->
            when (val log = entry.logItem) {
                is Event -> log.eventType.applyToMetrics(this)
                is BackgroundTaskCompletion -> log.backgroundTaskTicks.applyToMetrics(this)
                is ResultReceived -> updateTestResults(log.result, log.testKitType, log.testOrderType)
                is UpdateNetworkStats -> updateNetworkStats(log.downloadedBytes, log.uploadedBytes)
                is ExposureWindowMatched -> updateTotalExposureWindows(
                    log.totalRiskyExposures,
                    log.totalNonRiskyExposures
                )
            }
        }
    }
}

private fun Metrics.updateMissingPacketsLast7Days(missingSubmissionDays: Int) {
    missingPacketsLast7Days = missingSubmissionDays
}

private fun Metrics.updateTestResults(
    result: VirologyTestResult,
    testKitType: VirologyTestKitType,
    testOrderType: TestOrderType
) {
    when (result) {
        VOID -> when (testKitType) {
            LAB_RESULT -> when (testOrderType) {
                INSIDE_APP -> receivedVoidTestResultViaPolling++
                OUTSIDE_APP -> receivedVoidTestResultEnteredManually++
            }
            RAPID_RESULT -> when (testOrderType) {
                INSIDE_APP -> receivedVoidLFDTestResultViaPolling++
                OUTSIDE_APP -> receivedVoidLFDTestResultEnteredManually++
            }
            RAPID_SELF_REPORTED -> {}
        }
        POSITIVE -> when (testKitType) {
            LAB_RESULT -> when (testOrderType) {
                INSIDE_APP -> receivedPositiveTestResultViaPolling++
                OUTSIDE_APP -> receivedPositiveTestResultEnteredManually++
            }
            RAPID_RESULT -> when (testOrderType) {
                INSIDE_APP -> receivedPositiveLFDTestResultViaPolling++
                OUTSIDE_APP -> receivedPositiveLFDTestResultEnteredManually++
            }
            RAPID_SELF_REPORTED -> when (testOrderType) {
                INSIDE_APP -> {}
                OUTSIDE_APP -> receivedPositiveSelfRapidTestResultEnteredManually++
            }
        }
        NEGATIVE -> when (testKitType) {
            LAB_RESULT -> when (testOrderType) {
                INSIDE_APP -> receivedNegativeTestResultViaPolling++
                OUTSIDE_APP -> receivedNegativeTestResultEnteredManually++
            }
            RAPID_RESULT -> when (testOrderType) {
                INSIDE_APP -> receivedNegativeLFDTestResultViaPolling++
                OUTSIDE_APP -> receivedNegativeLFDTestResultEnteredManually++
            }
            RAPID_SELF_REPORTED -> {}
        }
        PLOD -> {}
    }
}

private fun Metrics.updateNetworkStats(downloadedBytes: Int?, uploadedBytes: Int?) {
    cumulativeDownloadBytes = cumulativeDownloadBytes plus downloadedBytes
    cumulativeUploadBytes = cumulativeUploadBytes plus uploadedBytes
}

private fun Metrics.updateTotalExposureWindows(totalRiskyExposures: Int, totalNonRiskyExposures: Int) {
    totalExposureWindowsNotConsideredRisky += totalNonRiskyExposures
    totalExposureWindowsConsideredRisky += totalRiskyExposures
}

private infix fun Int?.plus(other: Int?): Int? =
    this?.let { first ->
        other?.let { second -> first + second } ?: first
    } ?: other
