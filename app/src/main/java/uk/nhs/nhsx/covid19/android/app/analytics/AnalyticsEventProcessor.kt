package uk.nhs.nhsx.covid19.android.app.analytics

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OnboardingCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsEventProcessor(
    private val analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
    private val stateStorage: StateStorage,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val networkTrafficStats: NetworkTrafficStats,
    private val clock: Clock
) {

    @Inject
    constructor(
        analyticsMetricsLogStorage: AnalyticsMetricsLogStorage,
        stateStorage: StateStorage,
        exposureNotificationApi: ExposureNotificationApi,
        appAvailabilityProvider: AppAvailabilityProvider,
        networkTrafficStats: NetworkTrafficStats
    ) : this(
        analyticsMetricsLogStorage,
        stateStorage,
        exposureNotificationApi,
        appAvailabilityProvider,
        networkTrafficStats,
        Clock.systemUTC()
    )

    suspend fun track(analyticsEvent: AnalyticsEvent) {
        Timber.d("processing event: $analyticsEvent")
        val metrics = Metrics().apply {
            when (analyticsEvent) {
                OnboardingCompletion -> completedOnboarding = 1
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
                    if (currentState.isContactCaseOnly()) hasHadRiskyContactBackgroundTick = 1
                    if (currentState.isIndexCaseOnly()) hasSelfDiagnosedPositiveBackgroundTick = 1
                }

                if (!exposureNotificationApi.isEnabled()) encounterDetectionPausedBackgroundTick = 1
            }
            totalBackgroundTasks = 1
        }
    }
}
