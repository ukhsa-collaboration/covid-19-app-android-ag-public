package uk.nhs.nhsx.covid19.android.app.analytics

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ExposureWindowsMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedIsolationPaymentsApplication
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedTestOrdering
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedActiveIpcToken
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.RiskyContactReminderNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_TEST_ORDERING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_ACTIVE_IPC_TOKEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RISKY_CONTACT_REMINDER_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_ISOLATION_PAYMENTS_BUTTON
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.Companion.ISOLATION_STATE_CHANNEL_ID
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsEventProcessor @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val stateStorage: StateStorage,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val networkTrafficStats: NetworkTrafficStats,
    private val relevantTestResultProvider: RelevantTestResultProvider,
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val notificationProvider: NotificationProvider,
    private val clock: Clock
) {

    suspend fun track(analyticsEvent: AnalyticsEvent) {
        Timber.d("processing event: $analyticsEvent")

        val logItem = analyticsEvent.toAnalyticsLogItem()

        analyticsLogStorage.add(AnalyticsLogEntry(Instant.now(clock), logItem))
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
        is ExposureWindowsMatched -> AnalyticsLogItem.ExposureWindowMatched(totalRiskyExposures, totalNonRiskyExposures)
        ReceivedUnconfirmedPositiveTestResult -> Event(RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT)
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

            val currentState = stateStorage.state

            if (currentState is Isolation) {
                isIsolatingBackgroundTick = true
                isIsolatingForHadRiskyContactBackgroundTick = currentState.isContactCase()
                hasSelfDiagnosedPositiveBackgroundTick = currentState.isIndexCase()
                isIsolatingForSelfDiagnosedBackgroundTick = currentState.isSelfAssessmentIndexCase()

                relevantTestResultProvider.getTestResultIfPositive()?.let { acknowledgedPositiveTestResult ->
                    val isolationStartDate =
                        currentState.isolationStart.truncatedTo(ChronoUnit.DAYS)
                    val testResultAcknowledgeDate =
                        acknowledgedPositiveTestResult.acknowledgedDate.truncatedTo(ChronoUnit.DAYS)

                    if (testResultAcknowledgeDate.isEqualOrAfter(isolationStartDate)) {
                        val testKitType = acknowledgedPositiveTestResult.testKitType
                        isIsolatingForTestedPositiveBackgroundTick = testKitType == LAB_RESULT
                        isIsolatingForTestedLFDPositiveBackgroundTick =
                            testKitType == RAPID_RESULT ||
                            testKitType == RAPID_SELF_REPORTED
                        isIsolatingForUnconfirmedTestBackgroundTick =
                            acknowledgedPositiveTestResult.requiresConfirmatoryTest
                    }
                }
            }

            val recentIsolation = when (currentState) {
                is Isolation -> currentState
                is Default -> currentState.previousIsolation
            }

            recentIsolation?.let {
                hasHadRiskyContactBackgroundTick = it.isContactCase()
                hasSelfDiagnosedBackgroundTick = it.isSelfAssessmentIndexCase()
                relevantTestResultProvider.getTestResultIfPositive()?.let { acknowledgedPositiveTestResult ->
                    val testKitType = acknowledgedPositiveTestResult.testKitType
                    hasTestedPositiveBackgroundTick = testKitType == LAB_RESULT
                    hasTestedLFDPositiveBackgroundTick = testKitType == RAPID_RESULT ||
                        testKitType == RAPID_SELF_REPORTED
                }
            }

            haveActiveIpcTokenBackgroundTick =
                isolationPaymentTokenStateProvider.tokenState is IsolationPaymentTokenState.Token

            encounterDetectionPausedBackgroundTick = !exposureNotificationApi.isEnabled()

            hasRiskyContactNotificationsEnabledBackgroundTick = notificationProvider.isChannelEnabled(
                ISOLATION_STATE_CHANNEL_ID
            )
        }
}
