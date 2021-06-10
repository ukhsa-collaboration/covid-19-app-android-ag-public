package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ExposureWindowMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ASKED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_REMINDER_SCREEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DECLARED_NEGATIVE_RESULT_FROM_DCT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_LOCAL_INFO_SCREEN_VIA_BANNER
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_LOCAL_INFO_SCREEN_VIA_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_SEND_LOCAL_INFO_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_TEST_ORDERING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_ACTIVE_IPC_TOKEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M1_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M2_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RISKY_CONTACT_REMINDER_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_ISOLATION_PAYMENTS_BUTTON
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SUCCESSFULLY_SHARED_EXPOSURE_KEYS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.TOTAL_ALARM_MANAGER_BACKGROUND_TASKS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.TOTAL_SHARE_EXPOSURE_KEYS_REMINDER_NOTIFICATIONS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.util.toInt

fun List<AnalyticsLogEntry>.toMetrics(missingSubmissionDays: Int): Metrics {
    return Metrics().apply {
        updateMissingPacketsLast7Days(missingSubmissionDays)
        forEach { entry ->
            when (val log = entry.logItem) {
                is Event -> updateRegularEvent(log.eventType)
                is BackgroundTaskCompletion -> updateBackgroundTaskTicks(log.backgroundTaskTicks)
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

private fun Metrics.updateRegularEvent(eventType: RegularAnalyticsEventType) {
    when (eventType) {
        ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT -> acknowledgedStartOfIsolationDueToRiskyContact++
        QR_CODE_CHECK_IN -> checkedIn++
        CANCELED_CHECK_IN -> canceledCheckIn++
        COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION -> completedQuestionnaireAndStartedIsolation++
        COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION -> completedQuestionnaireButDidNotStartIsolation++
        POSITIVE_RESULT_RECEIVED -> receivedPositiveTestResult++
        NEGATIVE_RESULT_RECEIVED -> receivedNegativeTestResult++
        VOID_RESULT_RECEIVED -> receivedVoidTestResult++
        RECEIVED_RISKY_CONTACT_NOTIFICATION -> receivedRiskyContactNotification = 1
        RISKY_CONTACT_REMINDER_NOTIFICATION -> totalRiskyContactReminderNotifications++
        STARTED_ISOLATION -> startedIsolation++
        RECEIVED_ACTIVE_IPC_TOKEN -> receivedActiveIpcToken++
        SELECTED_ISOLATION_PAYMENTS_BUTTON -> selectedIsolationPaymentsButton++
        LAUNCHED_ISOLATION_PAYMENTS_APPLICATION -> launchedIsolationPaymentsApplication++
        LAUNCHED_TEST_ORDERING -> launchedTestOrdering++
        RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT -> receivedUnconfirmedPositiveTestResult++
        DECLARED_NEGATIVE_RESULT_FROM_DCT -> declaredNegativeResultFromDCT++
        DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT -> didHaveSymptomsBeforeReceivedTestResult++
        DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT -> didRememberOnsetSymptomsDateBeforeReceivedTestResult++
        DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY -> didAskForSymptomsOnPositiveTestEntry++
        RECEIVED_RISKY_VENUE_M1_WARNING -> receivedRiskyVenueM1Warning++
        RECEIVED_RISKY_VENUE_M2_WARNING -> receivedRiskyVenueM2Warning++
        TOTAL_ALARM_MANAGER_BACKGROUND_TASKS -> totalAlarmManagerBackgroundTasks++
        ASKED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW -> askedToShareExposureKeysInTheInitialFlow++
        CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW -> consentedToShareExposureKeysInTheInitialFlow++
        TOTAL_SHARE_EXPOSURE_KEYS_REMINDER_NOTIFICATIONS -> totalShareExposureKeysReminderNotifications++
        CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_REMINDER_SCREEN -> consentedToShareExposureKeysInReminderScreen++
        SUCCESSFULLY_SHARED_EXPOSURE_KEYS -> successfullySharedExposureKeys++
        DID_SEND_LOCAL_INFO_NOTIFICATION -> didSendLocalInfoNotification++
        DID_ACCESS_LOCAL_INFO_SCREEN_VIA_NOTIFICATION -> didAccessLocalInfoScreenViaNotification++
        DID_ACCESS_LOCAL_INFO_SCREEN_VIA_BANNER -> didAccessLocalInfoScreenViaBanner++
    }
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

private fun Metrics.updateBackgroundTaskTicks(backgroundTaskTicks: BackgroundTaskTicks) {
    totalBackgroundTasks++
    runningNormallyBackgroundTick += backgroundTaskTicks.runningNormallyBackgroundTick.toInt()
    isIsolatingBackgroundTick += backgroundTaskTicks.isIsolatingBackgroundTick.toInt()
    isIsolatingForHadRiskyContactBackgroundTick += backgroundTaskTicks.isIsolatingForHadRiskyContactBackgroundTick.toInt()
    hasSelfDiagnosedPositiveBackgroundTick += backgroundTaskTicks.hasSelfDiagnosedPositiveBackgroundTick.toInt()
    isIsolatingForSelfDiagnosedBackgroundTick += backgroundTaskTicks.isIsolatingForSelfDiagnosedBackgroundTick.toInt()
    isIsolatingForTestedPositiveBackgroundTick += backgroundTaskTicks.isIsolatingForTestedPositiveBackgroundTick.toInt()
    isIsolatingForTestedLFDPositiveBackgroundTick += backgroundTaskTicks.isIsolatingForTestedLFDPositiveBackgroundTick.toInt()
    isIsolatingForTestedSelfRapidPositiveBackgroundTick += backgroundTaskTicks.isIsolatingForTestedSelfRapidPositiveBackgroundTick.toInt()
    isIsolatingForUnconfirmedTestBackgroundTick += backgroundTaskTicks.isIsolatingForUnconfirmedTestBackgroundTick.toInt()
    hasHadRiskyContactBackgroundTick += backgroundTaskTicks.hasHadRiskyContactBackgroundTick.toInt()
    hasRiskyContactNotificationsEnabledBackgroundTick += backgroundTaskTicks.hasRiskyContactNotificationsEnabledBackgroundTick.toInt()
    hasSelfDiagnosedBackgroundTick += backgroundTaskTicks.hasSelfDiagnosedBackgroundTick.toInt()
    hasTestedPositiveBackgroundTick += backgroundTaskTicks.hasTestedPositiveBackgroundTick.toInt()
    hasTestedLFDPositiveBackgroundTick += backgroundTaskTicks.hasTestedLFDPositiveBackgroundTick.toInt()
    hasTestedSelfRapidPositiveBackgroundTick += backgroundTaskTicks.hasTestedSelfRapidPositiveBackgroundTick.toInt()
    encounterDetectionPausedBackgroundTick += backgroundTaskTicks.encounterDetectionPausedBackgroundTick.toInt()
    haveActiveIpcTokenBackgroundTick += backgroundTaskTicks.haveActiveIpcTokenBackgroundTick.toInt()
    hasReceivedRiskyVenueM2WarningBackgroundTick += backgroundTaskTicks.hasReceivedRiskyVenueM2WarningBackgroundTick.toInt()
    isDisplayingLocalInfoBackgroundTick += backgroundTaskTicks.isDisplayingLocalInfoBackgroundTick.toInt()
}

private infix fun Int?.plus(other: Int?): Int? =
    this?.let { first ->
        other?.let { second -> first + second } ?: first
    } ?: other
