package uk.nhs.nhsx.covid19.android.app.analytics

import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedIsolationPaymentsApplication
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedActiveIpcToken
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_ACTIVE_IPC_TOKEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_ISOLATION_PAYMENTS_BUTTON
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter

@Singleton
class AnalyticsEventProcessor @Inject constructor(
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val stateStorage: StateStorage,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val networkTrafficStats: NetworkTrafficStats,
    private val testResultsProvider: TestResultsProvider,
    private val isolationPaymentTokenStateProvider: IsolationPaymentTokenStateProvider,
    private val clock: Clock
) {

    suspend fun track(analyticsEvent: AnalyticsEvent) {
        Timber.d("processing event: $analyticsEvent")

        val logItem = analyticsEvent.toAnalyticsLogItem()

        analyticsLogStorage.add(AnalyticsLogEntry(Instant.now(clock), logItem))
    }

    private suspend fun AnalyticsEvent.toAnalyticsLogItem() = when (this) {
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
        StartedIsolation -> Event(STARTED_ISOLATION)
        is ResultReceived -> AnalyticsLogItem.ResultReceived(result, testOrderType)
        UpdateNetworkStats -> updateNetworkStats()
        ReceivedActiveIpcToken -> Event(RECEIVED_ACTIVE_IPC_TOKEN)
        SelectedIsolationPaymentsButton -> Event(SELECTED_ISOLATION_PAYMENTS_BUTTON)
        LaunchedIsolationPaymentsApplication -> Event(LAUNCHED_ISOLATION_PAYMENTS_APPLICATION)
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

            runningNormallyBackgroundTick = true

            val currentState = stateStorage.state

            if (currentState is Isolation) {
                isIsolatingBackgroundTick = true
                isIsolatingForHadRiskyContactBackgroundTick = currentState.isContactCase()
                hasSelfDiagnosedPositiveBackgroundTick = currentState.isIndexCase()
                isIsolatingForSelfDiagnosedBackgroundTick = currentState.isSelfAssessmentIndexCase()

                val lastAcknowledgePositiveTestResult = lastAcknowledgedPositiveTestResult()
                if (lastAcknowledgePositiveTestResult != null) {
                    val isolationStartDate =
                        currentState.isolationStart.truncatedTo(ChronoUnit.DAYS)
                    val testResultAcknowledgeDate =
                        lastAcknowledgePositiveTestResult.acknowledgedDate!!.truncatedTo(ChronoUnit.DAYS)
                    isIsolatingForTestedPositiveBackgroundTick =
                        testResultAcknowledgeDate.isEqualOrAfter(isolationStartDate)
                }
            }

            val recentIsolation = when (currentState) {
                is Isolation -> currentState
                is Default -> currentState.previousIsolation
            }

            recentIsolation?.let {
                hasHadRiskyContactBackgroundTick = it.isContactCase()
                hasSelfDiagnosedBackgroundTick = it.isSelfAssessmentIndexCase()
                hasTestedPositiveBackgroundTick = lastAcknowledgedPositiveTestResult() != null
            }

            haveActiveIpcTokenBackgroundTick = isolationPaymentTokenStateProvider.tokenState is IsolationPaymentTokenState.Token

            encounterDetectionPausedBackgroundTick = !exposureNotificationApi.isEnabled()
        }

    private fun lastAcknowledgedPositiveTestResult(): ReceivedTestResult? =
        testResultsProvider.testResults.values
            .filter { it.testResult == POSITIVE && it.acknowledgedDate != null }
            .maxBy { it.acknowledgedDate!! }
}
