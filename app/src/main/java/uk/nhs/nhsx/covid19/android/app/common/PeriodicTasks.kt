package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.CONNECTED
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTask.CLEAR_OUTDATED_DATA
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import javax.inject.Inject

class PeriodicTasks @Inject constructor(private val context: Context) {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(CONNECTED)
        .build()

    fun schedule() {
        cancelLegacyWorks()

        val downloadTasksWorkRequest =
            PeriodicWorkRequestBuilder<DownloadTasksWorker>(2, HOURS)
                .setConstraints(constraints)
                .build()

        val clearOutdatedData =
            PeriodicWorkRequestBuilder<ClearOutdatedDataWorker>(1, DAYS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).apply {
            enqueueUniquePeriodicWork(
                PeriodicTask.PERIODIC_TASKS.workName,
                REPLACE,
                downloadTasksWorkRequest
            )
            enqueueUniquePeriodicWork(
                CLEAR_OUTDATED_DATA.workName,
                REPLACE,
                clearOutdatedData
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
    }
}

enum class PeriodicTask(val workName: String) {
    PERIODIC_TASKS("periodicTasks"),
    CLEAR_OUTDATED_DATA("clearOutdatedData")
}
