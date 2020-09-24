package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import javax.inject.Inject

class DownloadKeysWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var downloadAndProcessKeys: DownloadAndProcessKeys

    @Inject
    lateinit var appAvailabilityProvider: AppAvailabilityProvider

    @Inject
    lateinit var analyticsEventProcessor: AnalyticsEventProcessor

    @Inject
    lateinit var notificationProvider: NotificationProvider

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)

        if (!appAvailabilityProvider.isAppAvailable()) {
            return Result.retry()
        }

        analyticsEventProcessor.track(BackgroundTaskCompletion)

        setForeground()

        return downloadAndProcessKeys().toWorkerResult().also {
            Timber.d("downloadAndProcessKeys: $it")
        }
    }

    private suspend fun setForeground() {
        val updatingDatabaseNotification = notificationProvider.getUpdatingDatabaseNotification()
        val foregroundInfo =
            ForegroundInfo(NOTIFICATION_UPDATING_DATABASE_ID, updatingDatabaseNotification)
        setForeground(foregroundInfo)
    }

    companion object {
        const val NOTIFICATION_UPDATING_DATABASE_ID = 111
    }
}
