package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult

sealed class AnalyticsEvent {
    object AcknowledgedStartOfIsolationDueToRiskyContact : AnalyticsEvent()
    object QrCodeCheckIn : AnalyticsEvent()
    object CanceledCheckIn : AnalyticsEvent()
    object CompletedQuestionnaireAndStartedIsolation : AnalyticsEvent()
    object CompletedQuestionnaireButDidNotStartIsolation : AnalyticsEvent()
    object BackgroundTaskCompletion : AnalyticsEvent()
    object PositiveResultReceived : AnalyticsEvent()
    object NegativeResultReceived : AnalyticsEvent()
    object VoidResultReceived : AnalyticsEvent()
    object ReceivedRiskyContactNotification : AnalyticsEvent()
    object RiskyContactReminderNotification : AnalyticsEvent()
    object StartedIsolation : AnalyticsEvent()
    data class ResultReceived(
        val result: VirologyTestResult,
        val testKitType: VirologyTestKitType,
        val testOrderType: TestOrderType
    ) : AnalyticsEvent()
    object UpdateNetworkStats : AnalyticsEvent()
    object ReceivedActiveIpcToken : AnalyticsEvent()
    object SelectedIsolationPaymentsButton : AnalyticsEvent()
    object LaunchedIsolationPaymentsApplication : AnalyticsEvent()
    object LaunchedTestOrdering : AnalyticsEvent()
    data class ExposureWindowsMatched(val totalRiskyExposures: Int, val totalNonRiskyExposures: Int) : AnalyticsEvent()
    object ReceivedUnconfirmedPositiveTestResult : AnalyticsEvent()
    object DidHaveSymptomsBeforeReceivedTestResult : AnalyticsEvent()
    object DidRememberOnsetSymptomsDateBeforeReceivedTestResult : AnalyticsEvent()
    object DidAskForSymptomsOnPositiveTestEntry : AnalyticsEvent()
    object ReceivedRiskyVenueM1Warning : AnalyticsEvent()
    object ReceivedRiskyVenueM2Warning : AnalyticsEvent()
    object TotalAlarmManagerBackgroundTasks : AnalyticsEvent()
    object AskedToShareExposureKeysInTheInitialFlow : AnalyticsEvent()
    object ConsentedToShareExposureKeysInTheInitialFlow : AnalyticsEvent()
    object SuccessfullySharedExposureKeys : AnalyticsEvent()
    object TotalShareExposureKeysReminderNotifications : AnalyticsEvent()
    object ConsentedToShareExposureKeysInReminderScreen : AnalyticsEvent()
    object DidSendLocalInfoNotification : AnalyticsEvent()
    object DidAccessLocalInfoScreenViaNotification : AnalyticsEvent()
    object DidAccessLocalInfoScreenViaBanner : AnalyticsEvent()
    object PositiveLabResultAfterPositiveLFD : AnalyticsEvent()
    object NegativeLabResultAfterPositiveLFDWithinTimeLimit : AnalyticsEvent()
    object NegativeLabResultAfterPositiveLFDOutsideTimeLimit : AnalyticsEvent()
    object PositiveLabResultAfterPositiveSelfRapidTest : AnalyticsEvent()
    object NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit : AnalyticsEvent()
    object NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit : AnalyticsEvent()
    object DidAccessRiskyVenueM2Notification : AnalyticsEvent()
    object SelectedTakeTestM2Journey : AnalyticsEvent()
    object SelectedTakeTestLaterM2Journey : AnalyticsEvent()
    object SelectedHasSymptomsM2Journey : AnalyticsEvent()
    object SelectedHasNoSymptomsM2Journey : AnalyticsEvent()
    object SelectedLfdTestOrderingM2Journey : AnalyticsEvent()
    object SelectedHasLfdTestM2Journey : AnalyticsEvent()
    object OptedOutForContactIsolation : AnalyticsEvent()
}
