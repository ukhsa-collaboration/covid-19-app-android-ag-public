package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
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
    lateinit var submitAnalytics: SubmitAnalytics

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)

        val isOnboardingAnalyticsEvent =
            inputData.getBoolean(INPUT_KEY_IS_ONBOARDING_ANALYTICS_EVENT, false)

        return submitAnalytics.invoke(isOnboardingAnalyticsEvent).toWorkerResult()
    }

    class Scheduler @Inject constructor(val context: Context) {
        fun scheduleOnboardingAnalyticsEvent() {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(CONNECTED)
                .build()

            val submitAnalyticsWorker =
                OneTimeWorkRequestBuilder<SubmitOnboardingAnalyticsWorker>()
                    .setConstraints(constraints)
                    .setInputData(
                        Data.Builder().putBoolean(
                            INPUT_KEY_IS_ONBOARDING_ANALYTICS_EVENT,
                            true
                        ).build()
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "SubmitAnalyticsWorkerOnboardingFinished",
                    REPLACE,
                    submitAnalyticsWorker
                )
        }
    }

    companion object {
        private const val INPUT_KEY_IS_ONBOARDING_ANALYTICS_EVENT = "IS_ONBOARDING_ANALYTICS_EVENT"
    }
}
