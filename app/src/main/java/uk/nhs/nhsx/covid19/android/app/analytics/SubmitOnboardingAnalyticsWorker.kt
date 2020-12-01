package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import javax.inject.Inject

class SubmitOnboardingAnalyticsWorker(
    val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var submitOnboardingAnalytics: SubmitOnboardingAnalytics

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)

        return submitOnboardingAnalytics.invoke().toWorkerResult()
    }

    class Scheduler @Inject constructor(val context: Context) {
        fun scheduleOnboardingAnalyticsEvent() {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(CONNECTED)
                .build()

            val submitAnalyticsWorker =
                OneTimeWorkRequestBuilder<SubmitOnboardingAnalyticsWorker>()
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "SubmitAnalyticsWorkerOnboardingFinished",
                    REPLACE,
                    submitAnalyticsWorker
                )
        }
    }
}
