package uk.nhs.nhsx.covid19.android.app.status

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.status.localmessage.DownloadLocalMessagesWork
import javax.inject.Inject

class DownloadAreaInfoWorker(
    val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var downloadRiskyPostCodesWork: DownloadRiskyPostCodesWork

    @Inject
    lateinit var downloadLocalMessagesWork: DownloadLocalMessagesWork

    @Inject
    lateinit var appAvailabilityProvider: AppAvailabilityProvider

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)

        if (RuntimeBehavior.isFeatureEnabled(DECOMMISSIONING_CLOSURE_SCREEN)) {
            return Result.failure()
        }
        if (!appAvailabilityProvider.isAppAvailable()) {
            return Result.retry()
        }

        downloadRiskyPostCodesWork()
        downloadLocalMessagesWork()
        return Result.success()
    }
}
