package uk.nhs.nhsx.covid19.android.app.analytics

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OnboardingCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsEventProcessor @Inject constructor(
    private val analyticsMetricsStorage: AnalyticsMetricsStorage,
    private val stateStorage: StateStorage,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val appAvailabilityProvider: AppAvailabilityProvider
) {

    suspend fun track(analyticsEvent: AnalyticsEvent) {

        val updated = analyticsMetricsStorage.metrics.apply {
            when (analyticsEvent) {
                OnboardingCompletion -> completedOnboarding += 1
                QrCodeCheckIn -> checkedIn += 1
                CanceledCheckIn -> canceledCheckIn += 1
                CompletedQuestionnaireAndStartedIsolation -> completedQuestionnaireAndStartedIsolation += 1
                CompletedQuestionnaireButDidNotStartIsolation -> completedQuestionnaireButDidNotStartIsolation += 1
                BackgroundTaskCompletion -> backgroundTasksCount()
                PositiveResultReceived -> receivedPositiveTestResult += 1
                NegativeResultReceived -> receivedNegativeTestResult += 1
                VoidResultReceived -> receivedVoidTestResult += 1
            }
        }
        analyticsMetricsStorage.metrics = updated
    }

    private suspend fun Metrics.backgroundTasksCount() {
        apply {
            if (appAvailabilityProvider.isAppAvailable()) {

                runningNormallyBackgroundTick += 1

                val currentState = stateStorage.state

                if (currentState is Isolation) {
                    isIsolatingBackgroundTick += 1
                    if (currentState.isContactCaseOnly()) hasHadRiskyContactBackgroundTick += 1
                    if (currentState.isIndexCaseOnly()) hasSelfDiagnosedPositiveBackgroundTick += 1
                }

                if (!exposureNotificationApi.isEnabled()) encounterDetectionPausedBackgroundTick += 1
            }
            totalBackgroundTasks += 1
        }
    }
}
