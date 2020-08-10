package uk.nhs.nhsx.covid19.android.app.availability

import android.content.Context
import android.os.Build.VERSION
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType.CONNECTED
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.HOURS
import javax.inject.Inject

class AppAvailabilityWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var getAvailabilityStatus: GetAvailabilityStatus

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)
        val sdkVersion = VERSION.SDK_INT
        val appVersionCode = BuildConfig.VERSION_CODE
        Timber.d(" sdk = $sdkVersion version code = $appVersionCode")

        return getAvailabilityStatus.invoke().toWorkerResult()
    }

    companion object {
        fun schedule(context: Context, interval: Long = 2, timeUnit: TimeUnit = HOURS) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(CONNECTED)
                .build()

            val appAvailabilityWorker =
                PeriodicWorkRequestBuilder<AppAvailabilityWorker>(interval, timeUnit)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "AppAvailabilityWorker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    appAvailabilityWorker
                )
        }
    }
}
