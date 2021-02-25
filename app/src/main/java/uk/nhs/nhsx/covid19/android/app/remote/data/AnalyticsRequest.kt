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
    val postalDistrict: String,
    val localAuthority: String?
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
    var receivedVoidLFDTestResultEnteredManually: Int = 0,
    var receivedPositiveLFDTestResultEnteredManually: Int = 0,
    var receivedNegativeLFDTestResultEnteredManually: Int = 0,
    var receivedVoidLFDTestResultViaPolling: Int = 0,
    var receivedPositiveLFDTestResultViaPolling: Int = 0,
    var receivedNegativeLFDTestResultViaPolling: Int = 0,
    var receivedUnconfirmedPositiveTestResult: Int = 0,
    var runningNormallyBackgroundTick: Int = 0,
    var totalBackgroundTasks: Int = 0,
    var hasSelfDiagnosedBackgroundTick: Int = 0,
    var hasTestedPositiveBackgroundTick: Int = 0,
    var hasTestedLFDPositiveBackgroundTick: Int = 0,
    var isIsolatingForSelfDiagnosedBackgroundTick: Int = 0,
    var isIsolatingForTestedPositiveBackgroundTick: Int = 0,
    var isIsolatingForTestedLFDPositiveBackgroundTick: Int = 0,
    var isIsolatingForUnconfirmedTestBackgroundTick: Int = 0,
    var isIsolatingForHadRiskyContactBackgroundTick: Int = 0,
    var receivedRiskyContactNotification: Int = 0,
    var startedIsolation: Int = 0,
    var receivedActiveIpcToken: Int = 0,
    var haveActiveIpcTokenBackgroundTick: Int = 0,
    var selectedIsolationPaymentsButton: Int = 0,
    var launchedIsolationPaymentsApplication: Int = 0,
    var launchedTestOrdering: Int = 0,
    var totalExposureWindowsNotConsideredRisky: Int = 0,
    var totalExposureWindowsConsideredRisky: Int = 0,
    var acknowledgedStartOfIsolationDueToRiskyContact: Int = 0,
    var hasRiskyContactNotificationsEnabledBackgroundTick: Int = 0,
    var totalRiskyContactReminderNotifications: Int = 0,
    var declaredNegativeResultFromDCT: Int = 0,
    var didHaveSymptomsBeforeReceivedTestResult: Int = 0,
    var didRememberOnsetSymptomsDateBeforeReceivedTestResult: Int = 0,
    var didAskForSymptomsOnPositiveTestEntry: Int = 0,
)
