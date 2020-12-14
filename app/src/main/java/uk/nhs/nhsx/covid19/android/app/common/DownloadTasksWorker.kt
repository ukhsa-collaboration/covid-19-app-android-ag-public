package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result.Success
import androidx.work.WorkerParameters
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalytics
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.availability.GetAvailabilityStatus
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWork
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadAndProcessKeys
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWork
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import javax.inject.Inject

class DownloadTasksWorker(
    val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var getAvailabilityStatus: GetAvailabilityStatus

    @Inject
    lateinit var appAvailabilityProvider: AppAvailabilityProvider

    @Inject
    lateinit var analyticsEventProcessor: AnalyticsEventProcessor

    @Inject
    lateinit var downloadVirologyTestResultWork: DownloadVirologyTestResultWork

    @Inject
    lateinit var downloadRiskyPostCodesWork: DownloadRiskyPostCodesWork

    @Inject
    lateinit var downloadAndProcessRiskyVenues: DownloadAndProcessRiskyVenues

    @Inject
    lateinit var downloadAndProcessKeys: DownloadAndProcessKeys

    @Inject
    lateinit var clearOutdatedDataAndUpdateIsolationConfiguration: ClearOutdatedDataAndUpdateIsolationConfiguration

    @Inject
    lateinit var exposureNotificationWork: ExposureNotificationWork

    @Inject
    lateinit var notificationProvider: NotificationProvider

    @Inject
    lateinit var submitAnalytics: SubmitAnalytics

    @Inject
    lateinit var onboardingCompletedProvider: OnboardingCompletedProvider

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)
        Timber.d("Running DownloadTasksWorker")

        setForeground()

        val isOnboardingCompleted = onboardingCompletedProvider.value.defaultFalse()
        if (isOnboardingCompleted) {
            submitAnalytics()
        }

        getAvailabilityStatus()

        if (!appAvailabilityProvider.isAppAvailable() || !isOnboardingCompleted) {
            return Result.failure()
        }

        clearOutdatedDataAndUpdateIsolationConfiguration()
        exposureNotificationWork.handleMatchesFound()
        downloadAndProcessKeys()
        downloadVirologyTestResultWork()
        downloadRiskyPostCodesWork()
        downloadAndProcessRiskyVenues()

        analyticsEventProcessor.track(BackgroundTaskCompletion)

        Timber.d("Finishing DownloadTasksWorker")
        return Success.success()
    }

    private suspend fun setForeground() {
        val updatingDatabaseNotification = notificationProvider.getUpdatingDatabaseNotification()
        val foregroundInfo =
            ForegroundInfo(NOTIFICATION_UPDATING_DATABASE_ID, updatingDatabaseNotification)
        setForeground(foregroundInfo)
    }

    companion object {
        private const val NOTIFICATION_UPDATING_DATABASE_ID = 111
    }
}
