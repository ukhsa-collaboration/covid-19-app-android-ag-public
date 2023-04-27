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
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import com.jeroenmols.featureflag.framework.RuntimeBehavior
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

        if (RuntimeBehavior.isFeatureEnabled(DECOMMISSIONING_CLOSURE_SCREEN)) {
            return Result.failure()
        }

        setForeground()

        val matchesFound = inputData.getBoolean(INPUT_MATCHES_FOUND, true)

        val result = if (matchesFound) {
            exposureNotificationWork.evaluateRisk()
        } else {
            exposureNotificationWork.doNotEvaluateRisk()
        }

        return result.toWorkerResult()
    }

    private suspend fun setForeground() {
        val updatingDatabaseNotification = notificationProvider.getUpdatingDatabaseNotification()
        val foregroundInfo =
            ForegroundInfo(NOTIFICATION_UPDATING_DATABASE_ID, updatingDatabaseNotification)
        setForeground(foregroundInfo)
    }

    companion object : ExposureNotificationWorkerScheduler {
        const val INPUT_MATCHES_FOUND = "INPUT_MATCHES_FOUND"
        private const val NOTIFICATION_UPDATING_DATABASE_ID = 112

        override fun scheduleEvaluateRisk(context: Context) {
            schedule(context, matchesFound = true)
        }

        override fun scheduleDoNotEvaluateRisk(context: Context) {
            schedule(context, matchesFound = false)
        }

        private fun schedule(context: Context, matchesFound: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(CONNECTED)
                .build()

            val exposureNotificationWork = OneTimeWorkRequestBuilder<ExposureNotificationWorker>()
                .setConstraints(constraints)
                .setInputData(Data.Builder().putBoolean(INPUT_MATCHES_FOUND, matchesFound).build())
                .build()

            WorkManager.getInstance(context).enqueue(exposureNotificationWork)
        }
    }
}

interface ExposureNotificationWorkerScheduler {
    fun scheduleEvaluateRisk(context: Context)
    fun scheduleDoNotEvaluateRisk(context: Context)
}
