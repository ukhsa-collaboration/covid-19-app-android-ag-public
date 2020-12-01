package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import javax.inject.Inject

class ExposureNotificationWorker(
    val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var exposureNotificationWork: ExposureNotificationWork

    @Inject
    lateinit var notificationProvider: NotificationProvider

    override suspend fun doWork(): Result {
        context.appComponent.inject(this)

        setForeground()
        val inputToken = inputData.getString(INPUT_TOKEN) ?: ""

        return exposureNotificationWork(inputToken).toWorkerResult()
    }

    private suspend fun setForeground() {
        val updatingDatabaseNotification = notificationProvider.getUpdatingDatabaseNotification()
        val foregroundInfo =
            ForegroundInfo(NOTIFICATION_UPDATING_DATABASE_ID, updatingDatabaseNotification)
        setForeground(foregroundInfo)
    }

    companion object : ExposureNotificationWorkerScheduler {
        const val INPUT_TOKEN = "INPUT_TOKEN"
        private const val NOTIFICATION_UPDATING_DATABASE_ID = 112

        override fun schedule(context: Context, token: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(CONNECTED)
                .build()

            val exposureNotificationWork = OneTimeWorkRequestBuilder<ExposureNotificationWorker>()
                .setConstraints(constraints)
                .setInputData(Data.Builder().putString(INPUT_TOKEN, token).build())
                .build()

            WorkManager.getInstance(context).enqueue(exposureNotificationWork)
        }
    }
}

interface ExposureNotificationWorkerScheduler {
    fun schedule(context: Context, token: String)
}
