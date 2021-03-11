package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult

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

enum class RegularAnalyticsEventType {
    ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT,
    QR_CODE_CHECK_IN,
    CANCELED_CHECK_IN,
    COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION,
    COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION,
    POSITIVE_RESULT_RECEIVED,
    NEGATIVE_RESULT_RECEIVED,
    VOID_RESULT_RECEIVED,
    RECEIVED_RISKY_CONTACT_NOTIFICATION,
    RISKY_CONTACT_REMINDER_NOTIFICATION,
    STARTED_ISOLATION,
    RECEIVED_ACTIVE_IPC_TOKEN,
    SELECTED_ISOLATION_PAYMENTS_BUTTON,
    LAUNCHED_ISOLATION_PAYMENTS_APPLICATION,
    LAUNCHED_TEST_ORDERING,
    RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT,
    DECLARED_NEGATIVE_RESULT_FROM_DCT,
    DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT,
    DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT,
    DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY,
    RECEIVED_RISKY_VENUE_M1_WARNING,
    RECEIVED_RISKY_VENUE_M2_WARNING,
    TOTAL_ALARM_MANAGER_BACKGROUND_TASKS
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
)
