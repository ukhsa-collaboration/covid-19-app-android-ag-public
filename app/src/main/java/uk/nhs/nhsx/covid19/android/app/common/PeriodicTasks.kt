package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.CONNECTED
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit.HOURS
import javax.inject.Inject

class PeriodicTasks @Inject constructor(private val context: Context) {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(CONNECTED)
        .build()

    fun schedule(policy: ExistingPeriodicWorkPolicy = REPLACE) {
        cancelLegacyWorks()

        val downloadTasksWorkRequest =
            PeriodicWorkRequestBuilder<DownloadTasksWorker>(2, HOURS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).apply {
            enqueueUniquePeriodicWork(
                PeriodicTask.PERIODIC_TASKS.workName,
                policy,
                downloadTasksWorkRequest
            )
        }
    }

    private fun cancelLegacyWorks() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("downloadDiagnosisKeysRequest")
        workManager.cancelUniqueWork("downloadRiskyPostCodesRequest")
        workManager.cancelUniqueWork("downloadAndProcessRiskyVenus")
        workManager.cancelUniqueWork("downloadAndProcessRiskyVenus")
        workManager.cancelUniqueWork("downloadVirologyTestResults")
        workManager.cancelUniqueWork("RiskyVenuePolling")
        workManager.cancelUniqueWork("SubmitAnalyticsWorker")
        workManager.cancelUniqueWork("AppAvailabilityWorker")
        workManager.cancelUniqueWork("clearOutdatedData")
    }
}

enum class PeriodicTask(val workName: String) {
    PERIODIC_TASKS("periodicTasks")
}
