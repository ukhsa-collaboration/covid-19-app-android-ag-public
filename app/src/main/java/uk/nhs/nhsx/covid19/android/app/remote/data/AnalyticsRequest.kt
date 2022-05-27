package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.util.adapters.SerializeNulls

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
    var receivedPositiveSelfRapidTestResultEnteredManually: Int = 0,
    var runningNormallyBackgroundTick: Int = 0,
    var totalBackgroundTasks: Int = 0,
    var hasSelfDiagnosedBackgroundTick: Int = 0,
    var hasTestedPositiveBackgroundTick: Int = 0,
    var hasTestedLFDPositiveBackgroundTick: Int = 0,
    var hasTestedSelfRapidPositiveBackgroundTick: Int = 0,
    var isIsolatingForSelfDiagnosedBackgroundTick: Int = 0,
    var isIsolatingForTestedPositiveBackgroundTick: Int = 0,
    var isIsolatingForTestedLFDPositiveBackgroundTick: Int = 0,
    var isIsolatingForTestedSelfRapidPositiveBackgroundTick: Int = 0,
    var isIsolatingForUnconfirmedTestBackgroundTick: Int = 0,
    @SerializeNulls var isIsolatingForHadRiskyContactBackgroundTick: Int? = 0,
    var receivedRiskyContactNotification: Int = 0,
    var startedIsolation: Int = 0,
    @SerializeNulls var receivedActiveIpcToken: Int? = 0,
    @SerializeNulls var haveActiveIpcTokenBackgroundTick: Int? = 0,
    @SerializeNulls var selectedIsolationPaymentsButton: Int? = 0,
    @SerializeNulls var launchedIsolationPaymentsApplication: Int? = 0,
    var launchedTestOrdering: Int = 0,
    var totalExposureWindowsNotConsideredRisky: Int = 0,
    var totalExposureWindowsConsideredRisky: Int = 0,
    @SerializeNulls var acknowledgedStartOfIsolationDueToRiskyContact: Int? = 0,
    var hasRiskyContactNotificationsEnabledBackgroundTick: Int = 0,
    var totalRiskyContactReminderNotifications: Int = 0,
    var didHaveSymptomsBeforeReceivedTestResult: Int = 0,
    var didRememberOnsetSymptomsDateBeforeReceivedTestResult: Int = 0,
    var didAskForSymptomsOnPositiveTestEntry: Int = 0,
    @SerializeNulls var receivedRiskyVenueM1Warning: Int? = 0,
    @SerializeNulls var receivedRiskyVenueM2Warning: Int? = 0,
    @SerializeNulls var hasReceivedRiskyVenueM2WarningBackgroundTick: Int? = 0,
    var totalAlarmManagerBackgroundTasks: Int = 0,
    var missingPacketsLast7Days: Int = 0,
    var askedToShareExposureKeysInTheInitialFlow: Int = 0,
    var consentedToShareExposureKeysInTheInitialFlow: Int = 0,
    var successfullySharedExposureKeys: Int = 0,
    var totalShareExposureKeysReminderNotifications: Int = 0,
    var consentedToShareExposureKeysInReminderScreen: Int = 0,
    var didSendLocalInfoNotification: Int = 0,
    var didAccessLocalInfoScreenViaNotification: Int = 0,
    var didAccessLocalInfoScreenViaBanner: Int = 0,
    var isDisplayingLocalInfoBackgroundTick: Int = 0,
    var positiveLabResultAfterPositiveLFD: Int = 0,
    var negativeLabResultAfterPositiveLFDWithinTimeLimit: Int = 0,
    var negativeLabResultAfterPositiveLFDOutsideTimeLimit: Int = 0,
    var positiveLabResultAfterPositiveSelfRapidTest: Int = 0,
    var negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit: Int = 0,
    var negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit: Int = 0,
    @SerializeNulls var didAccessRiskyVenueM2Notification: Int? = 0,
    @SerializeNulls var selectedTakeTestM2Journey: Int? = 0,
    @SerializeNulls var selectedTakeTestLaterM2Journey: Int? = 0,
    @SerializeNulls var selectedHasSymptomsM2Journey: Int? = 0,
    @SerializeNulls var selectedHasNoSymptomsM2Journey: Int? = 0,
    @SerializeNulls var selectedLFDTestOrderingM2Journey: Int? = 0,
    @SerializeNulls var selectedHasLFDTestM2Journey: Int? = 0,
    var optedOutForContactIsolation: Int = 0,
    var optedOutForContactIsolationBackgroundTick: Int = 0,
    var appIsUsableBackgroundTick: Int = 0,
    var appIsUsableBluetoothOffBackgroundTick: Int = 0,
    var appIsContactTraceableBackgroundTick: Int = 0,
    @SerializeNulls var didAccessSelfIsolationNoteLink: Int? = 0,
    var completedV2SymptomsQuestionnaire: Int = 0,
    var completedV2SymptomsQuestionnaireAndStayAtHome: Int = 0,
    var hasCompletedV2SymptomsQuestionnaireBackgroundTick: Int = 0,
    var hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick: Int = 0
)
