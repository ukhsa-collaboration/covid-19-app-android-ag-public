package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType.CONNECTED
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.HOURS
import javax.inject.Inject

class SubmitAnalyticsWorker(
    val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var submitAnalytics: SubmitAnalytics

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)

        return submitAnalytics.invoke().toWorkerResult()
    }

    companion object {
        fun schedule(
            context: Context,
            interval: Long = 24,
            timeUnit: TimeUnit = HOURS,
            replaceExisting: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(CONNECTED)
                .build()

            val submitAnalyticsWorker =
                PeriodicWorkRequestBuilder<SubmitAnalyticsWorker>(interval, timeUnit)
                    .setConstraints(constraints)
                    .build()

            val existingPeriodicWorkPolicy =
                if (replaceExisting) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "SubmitAnalyticsWorker",
                    existingPeriodicWorkPolicy,
                    submitAnalyticsWorker
                )
        }
    }
}
