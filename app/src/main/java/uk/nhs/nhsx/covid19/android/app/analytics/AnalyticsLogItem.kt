package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult

sealed class AnalyticsLogItem {
    @JsonClass(generateAdapter = true)
    data class Event(val eventType: RegularAnalyticsEventType) : AnalyticsLogItem()

    @JsonClass(generateAdapter = true)
    data class BackgroundTaskCompletion(val backgroundTaskTicks: BackgroundTaskTicks) : AnalyticsLogItem()

    @JsonClass(generateAdapter = true)
    data class ResultReceived(val result: VirologyTestResult, val testOrderType: TestOrderType) : AnalyticsLogItem()

    @JsonClass(generateAdapter = true)
    data class UpdateNetworkStats(val downloadedBytes: Int?, val uploadedBytes: Int?) : AnalyticsLogItem()
}

enum class RegularAnalyticsEventType {
    QR_CODE_CHECK_IN,
    CANCELED_CHECK_IN,
    COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION,
    COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION,
    POSITIVE_RESULT_RECEIVED,
    NEGATIVE_RESULT_RECEIVED,
    VOID_RESULT_RECEIVED,
    RECEIVED_RISKY_CONTACT_NOTIFICATION,
    STARTED_ISOLATION,
    RECEIVED_ACTIVE_IPC_TOKEN,
    SELECTED_ISOLATION_PAYMENTS_BUTTON,
    LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
}

@JsonClass(generateAdapter = true)
data class BackgroundTaskTicks(
    var runningNormallyBackgroundTick: Boolean = false,
    var isIsolatingBackgroundTick: Boolean = false,
    var isIsolatingForHadRiskyContactBackgroundTick: Boolean = false,
    var hasSelfDiagnosedPositiveBackgroundTick: Boolean = false,
    var isIsolatingForSelfDiagnosedBackgroundTick: Boolean = false,
    var isIsolatingForTestedPositiveBackgroundTick: Boolean = false,
    var hasHadRiskyContactBackgroundTick: Boolean = false,
    var hasSelfDiagnosedBackgroundTick: Boolean = false,
    var hasTestedPositiveBackgroundTick: Boolean = false,
    var encounterDetectionPausedBackgroundTick: Boolean = false,
    var haveActiveIpcTokenBackgroundTick: Boolean = false
)
