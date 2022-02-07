package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInReminderScreen
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaBanner
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessRiskyVenueM2Notification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessSelfIsolationNoteLink
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAskForSymptomsOnPositiveTestEntry
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidHaveSymptomsBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidRememberOnsetSymptomsDateBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidSendLocalInfoNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ExposureWindowsMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedIsolationPaymentsApplication
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedTestOrdering
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveLFDOutsideTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveLFDWithinTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OptedOutForContactIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveLabResultAfterPositiveLFD
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveLabResultAfterPositiveSelfRapidTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedActiveIpcToken
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM1Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM2Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.RiskyContactReminderNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasLfdTestM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasNoSymptomsM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasSymptomsM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedLfdTestOrderingM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedTakeTestLaterM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedTakeTestM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SuccessfullySharedExposureKeys
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.TotalAlarmManagerBackgroundTasks
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.TotalShareExposureKeysReminderNotifications
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ExposureWindowMatched
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ASKED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_REMINDER_SCREEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_LOCAL_INFO_SCREEN_VIA_BANNER
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_LOCAL_INFO_SCREEN_VIA_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_RISKY_VENUE_M2_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_SELF_ISOLATION_NOTE_LINK
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_SEND_LOCAL_INFO_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_TEST_ORDERING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_OUTSIDE_TIME_LIMIT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_WITHIN_TIME_LIMIT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_OUTSIDE_TIME_LIMIT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_WITHIN_TIME_LIMIT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.OPTED_OUT_FOR_CONTACT_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_LAB_RESULT_AFTER_POSITIVE_LFD
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_ACTIVE_IPC_TOKEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M1_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M2_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RISKY_CONTACT_REMINDER_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_HAS_LFD_TEST_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_HAS_NO_SYMPTOMS_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_HAS_SYMPTOMS_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_ISOLATION_PAYMENTS_BUTTON
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_LFD_TEST_ORDERING_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_TAKE_TEST_LATER_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_TAKE_TEST_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SUCCESSFULLY_SHARED_EXPOSURE_KEYS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.TOTAL_ALARM_MANAGER_BACKGROUND_TASKS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.TOTAL_SHARE_EXPOSURE_KEYS_REMINDER_NOTIFICATIONS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.Companion.ISOLATION_STATE_CHANNEL_ID
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.state.CreateIsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.QUESTIONNAIRE
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.status.localmessage.GetLocalMessageFromStorage
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AnalyticsEventProcessor @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val stateStorage: StateStorage,
    private val createIsolationLogicalState: CreateIsolationLogicalState,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val networkTrafficStats: NetworkTrafficStats,
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val notificationProvider: NotificationProvider,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val onboardingCompletedProvider: OnboardingCompletedProvider,
    private val getLocalMessageFromStorage: GetLocalMessageFromStorage,
    @Named(AppModule.BLUETOOTH_STATE_NAME) private val bluetoothAvailabilityStateProvider: AvailabilityStateProvider,
    @Named(AppModule.LOCATION_STATE_NAME) private val locationAvailabilityStateProvider: AvailabilityStateProvider,
    @Named(AppModule.APPLICATION_SCOPE) private val analyticsEventScope: CoroutineScope,
    private val clock: Clock
) {

    fun track(analyticsEvent: AnalyticsEvent) {
        val isOnboardingCompleted = onboardingCompletedProvider.value.defaultFalse()
        if (!isOnboardingCompleted) {
            return
        }

        Timber.d("processing event: $analyticsEvent")

        analyticsEventScope.launch {
            val logItem = analyticsEvent.toAnalyticsLogItem()
            analyticsLogStorage.add(AnalyticsLogEntry(Instant.now(clock), logItem))
        }
    }

    private suspend fun AnalyticsEvent.toAnalyticsLogItem() = when (this) {
        AcknowledgedStartOfIsolationDueToRiskyContact -> Event(ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT)
        QrCodeCheckIn -> Event(QR_CODE_CHECK_IN)
        CanceledCheckIn -> Event(CANCELED_CHECK_IN)
        CompletedQuestionnaireAndStartedIsolation -> Event(
            COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
        )
        CompletedQuestionnaireButDidNotStartIsolation -> Event(
            COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
        )
        BackgroundTaskCompletion -> AnalyticsLogItem.BackgroundTaskCompletion(getBackgroundTaskTicks())
        PositiveResultReceived -> Event(POSITIVE_RESULT_RECEIVED)
        NegativeResultReceived -> Event(NEGATIVE_RESULT_RECEIVED)
        VoidResultReceived -> Event(VOID_RESULT_RECEIVED)
        ReceivedRiskyContactNotification -> Event(RECEIVED_RISKY_CONTACT_NOTIFICATION)
        RiskyContactReminderNotification -> Event(RISKY_CONTACT_REMINDER_NOTIFICATION)
        StartedIsolation -> Event(STARTED_ISOLATION)
        is ResultReceived -> AnalyticsLogItem.ResultReceived(result, testKitType, testOrderType)
        UpdateNetworkStats -> updateNetworkStats()
        ReceivedActiveIpcToken -> Event(RECEIVED_ACTIVE_IPC_TOKEN)
        SelectedIsolationPaymentsButton -> Event(SELECTED_ISOLATION_PAYMENTS_BUTTON)
        LaunchedIsolationPaymentsApplication -> Event(LAUNCHED_ISOLATION_PAYMENTS_APPLICATION)
        LaunchedTestOrdering -> Event(LAUNCHED_TEST_ORDERING)
        is ExposureWindowsMatched -> ExposureWindowMatched(totalRiskyExposures, totalNonRiskyExposures)
        ReceivedUnconfirmedPositiveTestResult -> Event(RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT)
        DidHaveSymptomsBeforeReceivedTestResult -> Event(DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT)
        DidRememberOnsetSymptomsDateBeforeReceivedTestResult ->
            Event(DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT)
        DidAskForSymptomsOnPositiveTestEntry -> Event(DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY)
        ReceivedRiskyVenueM1Warning -> Event(RECEIVED_RISKY_VENUE_M1_WARNING)
        ReceivedRiskyVenueM2Warning -> Event(RECEIVED_RISKY_VENUE_M2_WARNING)
        TotalAlarmManagerBackgroundTasks -> Event(TOTAL_ALARM_MANAGER_BACKGROUND_TASKS)
        AskedToShareExposureKeysInTheInitialFlow -> Event(ASKED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW)
        ConsentedToShareExposureKeysInTheInitialFlow -> Event(CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW)
        ConsentedToShareExposureKeysInReminderScreen -> Event(CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_REMINDER_SCREEN)
        TotalShareExposureKeysReminderNotifications -> Event(TOTAL_SHARE_EXPOSURE_KEYS_REMINDER_NOTIFICATIONS)
        SuccessfullySharedExposureKeys -> Event(SUCCESSFULLY_SHARED_EXPOSURE_KEYS)
        DidSendLocalInfoNotification -> Event(DID_SEND_LOCAL_INFO_NOTIFICATION)
        DidAccessLocalInfoScreenViaNotification -> Event(DID_ACCESS_LOCAL_INFO_SCREEN_VIA_NOTIFICATION)
        DidAccessLocalInfoScreenViaBanner -> Event(DID_ACCESS_LOCAL_INFO_SCREEN_VIA_BANNER)
        PositiveLabResultAfterPositiveLFD -> Event(POSITIVE_LAB_RESULT_AFTER_POSITIVE_LFD)
        NegativeLabResultAfterPositiveLFDWithinTimeLimit ->
            Event(NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_WITHIN_TIME_LIMIT)
        NegativeLabResultAfterPositiveLFDOutsideTimeLimit ->
            Event(NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_OUTSIDE_TIME_LIMIT)
        PositiveLabResultAfterPositiveSelfRapidTest ->
            Event(POSITIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST)
        NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit ->
            Event(NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_WITHIN_TIME_LIMIT)
        NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit ->
            Event(NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_OUTSIDE_TIME_LIMIT)
        DidAccessRiskyVenueM2Notification -> Event(DID_ACCESS_RISKY_VENUE_M2_NOTIFICATION)
        SelectedTakeTestM2Journey -> Event(SELECTED_TAKE_TEST_M2_JOURNEY)
        SelectedTakeTestLaterM2Journey -> Event(SELECTED_TAKE_TEST_LATER_M2_JOURNEY)
        SelectedHasSymptomsM2Journey -> Event(SELECTED_HAS_SYMPTOMS_M2_JOURNEY)
        SelectedHasNoSymptomsM2Journey -> Event(SELECTED_HAS_NO_SYMPTOMS_M2_JOURNEY)
        SelectedLfdTestOrderingM2Journey -> Event(SELECTED_LFD_TEST_ORDERING_M2_JOURNEY)
        SelectedHasLfdTestM2Journey -> Event(SELECTED_HAS_LFD_TEST_M2_JOURNEY)
        OptedOutForContactIsolation -> Event(OPTED_OUT_FOR_CONTACT_ISOLATION)
        DidAccessSelfIsolationNoteLink -> Event(DID_ACCESS_SELF_ISOLATION_NOTE_LINK)
    }

    private fun updateNetworkStats() = AnalyticsLogItem.UpdateNetworkStats(
        downloadedBytes = networkTrafficStats.getTotalBytesDownloaded(),
        uploadedBytes = networkTrafficStats.getTotalBytesUploaded()
    )

    private suspend fun getBackgroundTaskTicks() =
        BackgroundTaskTicks().apply {
            if (!appAvailabilityProvider.isAppAvailable()) {
                return this
            }

            runningNormallyBackgroundTick = exposureNotificationApi.isRunningNormally()

            val locationState = locationAvailabilityStateProvider.getState()
            val locationServiceIsEnabledOrIsNotRequired = locationState == ENABLED || exposureNotificationApi.deviceSupportsLocationlessScanning()

            appIsUsableBackgroundTick = locationServiceIsEnabledOrIsNotRequired
            appIsUsableBluetoothOffBackgroundTick = bluetoothAvailabilityStateProvider.getState() == DISABLED && locationServiceIsEnabledOrIsNotRequired
            appIsContactTraceableBackgroundTick = exposureNotificationApi.isRunningNormally()

            val currentState = createIsolationLogicalState(stateStorage.state)

            if (currentState.isActiveIsolation(clock)) {
                isIsolatingBackgroundTick = true
                isIsolatingForHadRiskyContactBackgroundTick = currentState.isActiveContactCase(clock)

                isIsolatingForSelfDiagnosedBackgroundTick =
                    currentState.getActiveIndexCase(clock)?.isSelfAssessment() ?: false

                currentState.getActiveTestResultIfPositive(clock)?.let { acknowledgedActivePositiveTestResult ->
                    val testKitType = acknowledgedActivePositiveTestResult.testKitType
                    isIsolatingForTestedPositiveBackgroundTick = testKitType == LAB_RESULT || testKitType == null
                    isIsolatingForTestedLFDPositiveBackgroundTick = testKitType == RAPID_RESULT
                    isIsolatingForTestedSelfRapidPositiveBackgroundTick = testKitType == RAPID_SELF_REPORTED
                    isIsolatingForUnconfirmedTestBackgroundTick = !acknowledgedActivePositiveTestResult.isConfirmed()
                }
            }

            if (currentState is PossiblyIsolating) {
                hasHadRiskyContactBackgroundTick = currentState.remembersContactCase()
                hasSelfDiagnosedPositiveBackgroundTick = currentState.remembersIndexCase()
                hasSelfDiagnosedBackgroundTick = currentState.remembersIndexCaseWithSelfAssessment()
                currentState.getTestResultIfPositive()?.let { acknowledgedPositiveTestResult ->
                    val testKitType = acknowledgedPositiveTestResult.testKitType
                    hasTestedPositiveBackgroundTick = testKitType == LAB_RESULT || testKitType == null
                    hasTestedLFDPositiveBackgroundTick = testKitType == RAPID_RESULT
                    hasTestedSelfRapidPositiveBackgroundTick = testKitType == RAPID_SELF_REPORTED
                }
            }

            haveActiveIpcTokenBackgroundTick =
                isolationPaymentTokenStateProvider.tokenState is IsolationPaymentTokenState.Token

            encounterDetectionPausedBackgroundTick = !exposureNotificationApi.isEnabled()

            hasRiskyContactNotificationsEnabledBackgroundTick =
                notificationProvider.isChannelEnabled(ISOLATION_STATE_CHANNEL_ID)

            hasReceivedRiskyVenueM2WarningBackgroundTick =
                lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk()

            isDisplayingLocalInfoBackgroundTick = getLocalMessageFromStorage() != null

            optedOutForContactIsolationBackgroundTick =
                stateStorage.state.contact?.optOutOfContactIsolation?.reason == QUESTIONNAIRE
        }
}
