package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.toInt

sealed class AnalyticsLogItem {
    @JsonClass(generateAdapter = true)
    data class Event(val eventType: RegularAnalyticsEventType) : AnalyticsLogItem()

    @JsonClass(generateAdapter = true)
    data class BackgroundTaskCompletion(val backgroundTaskTicks: BackgroundTaskTicks) : AnalyticsLogItem()

    @JsonClass(generateAdapter = true)
    data class ResultReceived(
        val result: VirologyTestResult,
        val testKitType: VirologyTestKitType = LAB_RESULT,
        val testOrderType: TestOrderType
    ) : AnalyticsLogItem()

    @JsonClass(generateAdapter = true)
    data class UpdateNetworkStats(val downloadedBytes: Int?, val uploadedBytes: Int?) : AnalyticsLogItem()

    @JsonClass(generateAdapter = true)
    data class ExposureWindowMatched(val totalRiskyExposures: Int, val totalNonRiskyExposures: Int) : AnalyticsLogItem()
}

enum class RegularAnalyticsEventType(
    var applyToMetrics: (metrics: Metrics) -> Unit,
) {
    ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT({ it.acknowledgedStartOfIsolationDueToRiskyContact++ }),
    QR_CODE_CHECK_IN({ it.checkedIn++ }),
    CANCELED_CHECK_IN({ it.canceledCheckIn++ }),
    COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION({ it.completedQuestionnaireAndStartedIsolation++ }),
    COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION({ it.completedQuestionnaireButDidNotStartIsolation++ }),
    POSITIVE_RESULT_RECEIVED({ it.receivedPositiveTestResult++ }),
    NEGATIVE_RESULT_RECEIVED({ it.receivedNegativeTestResult++ }),
    VOID_RESULT_RECEIVED({ it.receivedVoidTestResult++ }),
    RECEIVED_RISKY_CONTACT_NOTIFICATION({ it.receivedRiskyContactNotification = 1 }),
    RISKY_CONTACT_REMINDER_NOTIFICATION({ it.totalRiskyContactReminderNotifications++ }),
    STARTED_ISOLATION({ it.startedIsolation++ }),
    RECEIVED_ACTIVE_IPC_TOKEN({ it.receivedActiveIpcToken++ }),
    SELECTED_ISOLATION_PAYMENTS_BUTTON({ it.selectedIsolationPaymentsButton++ }),
    LAUNCHED_ISOLATION_PAYMENTS_APPLICATION({ it.launchedIsolationPaymentsApplication++ }),
    LAUNCHED_TEST_ORDERING({ it.launchedTestOrdering++ }),
    RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT({ it.receivedUnconfirmedPositiveTestResult++ }),
    DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT({ it.didHaveSymptomsBeforeReceivedTestResult++ }),
    DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT({ it.didRememberOnsetSymptomsDateBeforeReceivedTestResult++ }),
    DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY({ it.didAskForSymptomsOnPositiveTestEntry++ }),
    RECEIVED_RISKY_VENUE_M1_WARNING({ it.receivedRiskyVenueM1Warning++ }),
    RECEIVED_RISKY_VENUE_M2_WARNING({ it.receivedRiskyVenueM2Warning++ }),
    TOTAL_ALARM_MANAGER_BACKGROUND_TASKS({ it.totalAlarmManagerBackgroundTasks++ }),
    ASKED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW({ it.askedToShareExposureKeysInTheInitialFlow++ }),
    CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW({ it.consentedToShareExposureKeysInTheInitialFlow++ }),
    TOTAL_SHARE_EXPOSURE_KEYS_REMINDER_NOTIFICATIONS({ it.totalShareExposureKeysReminderNotifications++ }),
    CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_REMINDER_SCREEN({ it.consentedToShareExposureKeysInReminderScreen++ }),
    SUCCESSFULLY_SHARED_EXPOSURE_KEYS({ it.successfullySharedExposureKeys++ }),
    DID_SEND_LOCAL_INFO_NOTIFICATION({ it.didSendLocalInfoNotification++ }),
    DID_ACCESS_LOCAL_INFO_SCREEN_VIA_NOTIFICATION({ it.didAccessLocalInfoScreenViaNotification++ }),
    DID_ACCESS_LOCAL_INFO_SCREEN_VIA_BANNER({ it.didAccessLocalInfoScreenViaBanner++ }),
    POSITIVE_LAB_RESULT_AFTER_POSITIVE_LFD({ it.positiveLabResultAfterPositiveLFD++ }),
    NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_WITHIN_TIME_LIMIT({ it.negativeLabResultAfterPositiveLFDWithinTimeLimit++ }),
    NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_OUTSIDE_TIME_LIMIT({ it.negativeLabResultAfterPositiveLFDOutsideTimeLimit++ }),
    POSITIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST({ it.positiveLabResultAfterPositiveSelfRapidTest++ }),
    NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_WITHIN_TIME_LIMIT({ it.negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit++ }),
    NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_OUTSIDE_TIME_LIMIT({ it.negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit++ }),
    DID_ACCESS_RISKY_VENUE_M2_NOTIFICATION({ it.didAccessRiskyVenueM2Notification++ }),
    SELECTED_TAKE_TEST_M2_JOURNEY({ it.selectedTakeTestM2Journey++ }),
    SELECTED_TAKE_TEST_LATER_M2_JOURNEY({ it.selectedTakeTestLaterM2Journey++ }),
    SELECTED_HAS_SYMPTOMS_M2_JOURNEY({ it.selectedHasSymptomsM2Journey++ }),
    SELECTED_HAS_NO_SYMPTOMS_M2_JOURNEY({ it.selectedHasNoSymptomsM2Journey++ }),
    SELECTED_LFD_TEST_ORDERING_M2_JOURNEY({ it.selectedLFDTestOrderingM2Journey++ }),
    SELECTED_HAS_LFD_TEST_M2_JOURNEY({ it.selectedHasLFDTestM2Journey++ }),
    OPTED_OUT_FOR_CONTACT_ISOLATION({ it.optedOutForContactIsolation++ }),
    DID_ACCESS_SELF_ISOLATION_NOTE_LINK({ it.didAccessSelfIsolationNoteLink++ }),
}

@JsonClass(generateAdapter = true)
data class BackgroundTaskTicks(
    var runningNormallyBackgroundTick: Boolean = false,
    var isIsolatingBackgroundTick: Boolean = false,
    var isIsolatingForHadRiskyContactBackgroundTick: Boolean = false,
    var hasSelfDiagnosedPositiveBackgroundTick: Boolean = false,
    var isIsolatingForSelfDiagnosedBackgroundTick: Boolean = false,
    var isIsolatingForTestedPositiveBackgroundTick: Boolean = false,
    var isIsolatingForTestedLFDPositiveBackgroundTick: Boolean = false,
    var isIsolatingForTestedSelfRapidPositiveBackgroundTick: Boolean = false,
    var hasHadRiskyContactBackgroundTick: Boolean = false,
    var hasRiskyContactNotificationsEnabledBackgroundTick: Boolean = false,
    var hasSelfDiagnosedBackgroundTick: Boolean = false,
    var hasTestedPositiveBackgroundTick: Boolean = false,
    var hasTestedLFDPositiveBackgroundTick: Boolean = false,
    var hasTestedSelfRapidPositiveBackgroundTick: Boolean = false,
    var encounterDetectionPausedBackgroundTick: Boolean = false,
    var haveActiveIpcTokenBackgroundTick: Boolean = false,
    var isIsolatingForUnconfirmedTestBackgroundTick: Boolean = false,
    var hasReceivedRiskyVenueM2WarningBackgroundTick: Boolean = false,
    var isDisplayingLocalInfoBackgroundTick: Boolean = false,
    var optedOutForContactIsolationBackgroundTick: Boolean = false,
    var appIsUsableBackgroundTick: Boolean = false,
    var appIsUsableBluetoothOffBackgroundTick: Boolean = false,
    var appIsContactTraceableBackgroundTick: Boolean = false
) {
    fun applyToMetrics(metrics: Metrics) {
        metrics.totalBackgroundTasks++
        metrics.runningNormallyBackgroundTick += runningNormallyBackgroundTick.toInt()
        metrics.isIsolatingBackgroundTick += isIsolatingBackgroundTick.toInt()
        metrics.isIsolatingForHadRiskyContactBackgroundTick += isIsolatingForHadRiskyContactBackgroundTick.toInt()
        metrics.hasSelfDiagnosedPositiveBackgroundTick += hasSelfDiagnosedPositiveBackgroundTick.toInt()
        metrics.isIsolatingForSelfDiagnosedBackgroundTick += isIsolatingForSelfDiagnosedBackgroundTick.toInt()
        metrics.isIsolatingForTestedPositiveBackgroundTick += isIsolatingForTestedPositiveBackgroundTick.toInt()
        metrics.isIsolatingForTestedLFDPositiveBackgroundTick += isIsolatingForTestedLFDPositiveBackgroundTick.toInt()
        metrics.isIsolatingForTestedSelfRapidPositiveBackgroundTick += isIsolatingForTestedSelfRapidPositiveBackgroundTick.toInt()
        metrics.isIsolatingForUnconfirmedTestBackgroundTick += isIsolatingForUnconfirmedTestBackgroundTick.toInt()
        metrics.hasHadRiskyContactBackgroundTick += hasHadRiskyContactBackgroundTick.toInt()
        metrics.hasRiskyContactNotificationsEnabledBackgroundTick += hasRiskyContactNotificationsEnabledBackgroundTick.toInt()
        metrics.hasSelfDiagnosedBackgroundTick += hasSelfDiagnosedBackgroundTick.toInt()
        metrics.hasTestedPositiveBackgroundTick += hasTestedPositiveBackgroundTick.toInt()
        metrics.hasTestedLFDPositiveBackgroundTick += hasTestedLFDPositiveBackgroundTick.toInt()
        metrics.hasTestedSelfRapidPositiveBackgroundTick += hasTestedSelfRapidPositiveBackgroundTick.toInt()
        metrics.encounterDetectionPausedBackgroundTick += encounterDetectionPausedBackgroundTick.toInt()
        metrics.haveActiveIpcTokenBackgroundTick += haveActiveIpcTokenBackgroundTick.toInt()
        metrics.hasReceivedRiskyVenueM2WarningBackgroundTick += hasReceivedRiskyVenueM2WarningBackgroundTick.toInt()
        metrics.isDisplayingLocalInfoBackgroundTick += isDisplayingLocalInfoBackgroundTick.toInt()
        metrics.optedOutForContactIsolationBackgroundTick += optedOutForContactIsolationBackgroundTick.toInt()
        metrics.appIsUsableBackgroundTick += appIsUsableBackgroundTick.toInt()
        metrics.appIsUsableBluetoothOffBackgroundTick += appIsUsableBluetoothOffBackgroundTick.toInt()
        metrics.appIsContactTraceableBackgroundTick += appIsContactTraceableBackgroundTick.toInt()
    }
}
