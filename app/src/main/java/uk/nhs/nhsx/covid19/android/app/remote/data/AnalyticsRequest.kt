package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnalyticsPayload(
    val analyticsWindow: AnalyticsWindow,
    val includesMultipleApplicationVersions: Boolean,
    val metadata: Metadata,
    val metrics: Metrics
)

@JsonClass(generateAdapter = true)
data class AnalyticsWindow(
    val endDate: String,
    val startDate: String
)

@JsonClass(generateAdapter = true)
data class Metadata(
    val deviceModel: String,
    val latestApplicationVersion: String,
    val operatingSystemVersion: String,
    val postalDistrict: String
)

@JsonClass(generateAdapter = true)
data class Metrics(
    var canceledCheckIn: Int = 0,
    var checkedIn: Int = 0,
    var completedOnboarding: Int = 0,
    var completedQuestionnaireAndStartedIsolation: Int = 0,
    var completedQuestionnaireButDidNotStartIsolation: Int = 0,
    var cumulativeDownloadBytes: Int? = null,
    var cumulativeUploadBytes: Int? = null,
    var encounterDetectionPausedBackgroundTick: Int = 0,
    var hasHadRiskyContactBackgroundTick: Int = 0,
    var hasSelfDiagnosedPositiveBackgroundTick: Int = 0,
    var isIsolatingBackgroundTick: Int = 0,
    var receivedNegativeTestResult: Int = 0,
    var receivedPositiveTestResult: Int = 0,
    var receivedVoidTestResult: Int = 0,
    var receivedVoidTestResultEnteredManually: Int = 0,
    var receivedPositiveTestResultEnteredManually: Int = 0,
    var receivedNegativeTestResultEnteredManually: Int = 0,
    var receivedVoidTestResultViaPolling: Int = 0,
    var receivedPositiveTestResultViaPolling: Int = 0,
    var receivedNegativeTestResultViaPolling: Int = 0,
    var runningNormallyBackgroundTick: Int = 0,
    var totalBackgroundTasks: Int = 0,
    var hasSelfDiagnosedBackgroundTick: Int = 0,
    var hasTestedPositiveBackgroundTick: Int = 0,
    var isIsolatingForSelfDiagnosedBackgroundTick: Int = 0,
    var isIsolatingForTestedPositiveBackgroundTick: Int = 0,
    var isIsolatingForHadRiskyContactBackgroundTick: Int = 0
)
