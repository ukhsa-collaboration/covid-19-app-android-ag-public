package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.NetworkType.CONNECTED
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.DecommissioningNotificationSentProvider
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTask.PERIODIC_TASKS
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import java.time.Duration
import java.util.concurrent.TimeUnit.HOURS
import javax.inject.Inject
import javax.inject.Named

class PeriodicTasks @Inject constructor(
    private val context: Context,
    private val decommissioningNotificationSentProvider: DecommissioningNotificationSentProvider,
    private val exposureNotificationApi: ExposureNotificationApi,
    @Named(AppModule.APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
) {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(CONNECTED)
        .build()

    fun schedule(policy: ExistingPeriodicWorkPolicy = REPLACE) {
        if (RuntimeBehavior.isFeatureEnabled(DECOMMISSIONING_CLOSURE_SCREEN)) {
            if (decommissioningNotificationSentProvider.value.defaultFalse()) {
                cancelLegacyWorks(shouldCancelPeriodicWork = true)
            } else {
                cancelLegacyWorks()
            }
            applicationScope.launch {
                stopExposureNotificationApi()
                Timber.d("exposure stop call returned")
            }
            return
        }

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

    private fun cancelLegacyWorks(shouldCancelPeriodicWork: Boolean = false) {
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
        if (shouldCancelPeriodicWork) {
            workManager.cancelUniqueWork(PERIODIC_TASKS.workName)
            workManager.cancelUniqueWork("SubmitAnalyticsWorkerOnboardingFinished")
        }
    }

    private suspend fun stopExposureNotificationApi() {
        try {
            withTimeout(Duration.ofSeconds(10).toMillis()) {
                Timber.d("exposure stop called")
                exposureNotificationApi.stop()
            }
        } catch (exception: Exception) {
            Timber.d(exception, "exposure stop call timed out")
        }
    }
}

enum class PeriodicTask(val workName: String) {
    PERIODIC_TASKS("periodicTasks")
}
