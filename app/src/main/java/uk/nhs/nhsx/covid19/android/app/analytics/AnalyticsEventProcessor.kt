package uk.nhs.nhsx.covid19.android.app.analytics

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter

@Singleton
class AnalyticsEventProcessor(
    private val analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
    private val stateStorage: StateStorage,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val networkTrafficStats: NetworkTrafficStats,
    private val testResultsProvider: TestResultsProvider,
    private val clock: Clock
) {

    @Inject
    constructor(
        analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
        stateStorage: StateStorage,
        exposureNotificationApi: ExposureNotificationApi,
        appAvailabilityProvider: AppAvailabilityProvider,
        networkTrafficStats: NetworkTrafficStats,
        testResultsProvider: TestResultsProvider
    ) : this(
        analyticsMetricsLogStorage,
        stateStorage,
        exposureNotificationApi,
        appAvailabilityProvider,
        networkTrafficStats,
        testResultsProvider,
        Clock.systemUTC()
    )

    suspend fun track(analyticsEvent: AnalyticsEvent) {
        Timber.d("processing event: $analyticsEvent")
        val metrics = Metrics().apply {
            when (analyticsEvent) {
                QrCodeCheckIn -> checkedIn = 1
                CanceledCheckIn -> canceledCheckIn = 1
                CompletedQuestionnaireAndStartedIsolation ->
                    completedQuestionnaireAndStartedIsolation =
                        1
                CompletedQuestionnaireButDidNotStartIsolation ->
                    completedQuestionnaireButDidNotStartIsolation =
                        1
                BackgroundTaskCompletion -> backgroundTasksCount()
                PositiveResultReceived -> receivedPositiveTestResult = 1
                NegativeResultReceived -> receivedNegativeTestResult = 1
                VoidResultReceived -> receivedVoidTestResult = 1
                is ResultReceived -> updateTestResults(analyticsEvent.result, analyticsEvent.testOrderType)
                UpdateNetworkStats -> updateNetworkStats()
            }
        }

        analyticsMetricsLogStorage.add(MetricsLogEntry(metrics, Instant.now(clock)))
    }

    private fun Metrics.updateNetworkStats() {
        apply {
            cumulativeDownloadBytes = networkTrafficStats.getTotalBytesDownloaded()
            cumulativeUploadBytes = networkTrafficStats.getTotalBytesUploaded()
        }
    }

    private suspend fun Metrics.backgroundTasksCount() {
        apply {
            if (appAvailabilityProvider.isAppAvailable()) {

                runningNormallyBackgroundTick = 1

                val currentState = stateStorage.state

                if (currentState is Isolation) {
                    isIsolatingBackgroundTick = 1
                    if (currentState.isContactCase()) isIsolatingForHadRiskyContactBackgroundTick = 1
                    if (currentState.isIndexCase()) hasSelfDiagnosedPositiveBackgroundTick = 1
                    if (currentState.isSelfAssessmentIndexCase()) isIsolatingForSelfDiagnosedBackgroundTick = 1

                    val lastAcknowledgePositiveTestResult = lastAcknowledgedPositiveTestResult()
                    if (lastAcknowledgePositiveTestResult != null) {
                        val isolationStartDate = currentState.isolationStart.truncatedTo(ChronoUnit.DAYS)
                        val testResultAcknowledgeDate = lastAcknowledgePositiveTestResult.acknowledgedDate!!.truncatedTo(ChronoUnit.DAYS)
                        if (testResultAcknowledgeDate.isEqualOrAfter(isolationStartDate)) {
                            isIsolatingForTestedPositiveBackgroundTick = 1
                        }
                    }
                }

                val recentIsolation = when (currentState) {
                    is Isolation -> currentState
                    is Default -> currentState.previousIsolation
                }
                recentIsolation?.let {
                    if (it.isContactCase()) hasHadRiskyContactBackgroundTick = 1
                    if (it.isSelfAssessmentIndexCase()) hasSelfDiagnosedBackgroundTick = 1
                    if (lastAcknowledgedPositiveTestResult() != null) hasTestedPositiveBackgroundTick = 1
                }

                if (!exposureNotificationApi.isEnabled()) encounterDetectionPausedBackgroundTick = 1
            }
            totalBackgroundTasks = 1
        }
    }

    private fun Metrics.updateTestResults(result: VirologyTestResult, testOrderType: TestOrderType) {
        apply {
            when {
                result == VOID && testOrderType == INSIDE_APP -> receivedVoidTestResultViaPolling = 1
                result == VOID && testOrderType == OUTSIDE_APP -> receivedVoidTestResultEnteredManually = 1
                result == POSITIVE && testOrderType == INSIDE_APP -> receivedPositiveTestResultViaPolling = 1
                result == POSITIVE && testOrderType == OUTSIDE_APP -> receivedPositiveTestResultEnteredManually = 1
                result == NEGATIVE && testOrderType == INSIDE_APP -> receivedNegativeTestResultViaPolling = 1
                result == NEGATIVE && testOrderType == OUTSIDE_APP -> receivedNegativeTestResultEnteredManually = 1
            }
        }
    }

    private fun lastAcknowledgedPositiveTestResult(): ReceivedTestResult? =
        testResultsProvider.testResults.values
            .filter { it.testResult == POSITIVE && it.acknowledgedDate != null }
            .maxBy { it.acknowledgedDate!! }
}
