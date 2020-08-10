package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import javax.inject.Inject

class ClearOutdatedDataWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var clearOutdatedDataAndUpdateIsolationConfiguration: ClearOutdatedDataAndUpdateIsolationConfiguration

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)
        return clearOutdatedDataAndUpdateIsolationConfiguration.doWork().toWorkerResult()
    }
}
