package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.BackoffPolicy.EXPONENTIAL
import androidx.work.BackoffPolicy.LINEAR
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTask.CLEAR_OUTDATED_DATA
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTask.RISKY_VENUE_POLLING
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitKeysWorker
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInitialWorker
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposurePollingCircuitBreakerWorker
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysWorker
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenuesWorker
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenuesCircuitBreakerPollingWorker
import uk.nhs.nhsx.covid19.android.app.remote.data.TemporaryExposureKeysPayload
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWorker
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWorker
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Inject

class PeriodicTasks @Inject constructor(
    private val context: Context,
    private val moshi: Moshi
) {
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(CONNECTED)
        .build()

    fun schedule(keepPrevious: Boolean, interval: Long = 2, timeUnit: TimeUnit = HOURS) {
        val downloadDiagnosisKeysRequest =
            PeriodicWorkRequestBuilder<DownloadKeysWorker>(interval, timeUnit)
                .setConstraints(constraints)
                .build()

        val downloadRiskyPostCodesRequest =
            PeriodicWorkRequestBuilder<DownloadRiskyPostCodesWorker>(interval, timeUnit)
                .setConstraints(constraints)
                .build()

        val downloadAndProcessRiskyVenus =
            PeriodicWorkRequestBuilder<DownloadAndProcessRiskyVenuesWorker>(interval, timeUnit)
                .setConstraints(constraints)
                .build()

        val downloadVirologyTestResults =
            PeriodicWorkRequestBuilder<DownloadVirologyTestResultWorker>(interval, timeUnit)
                .setConstraints(constraints)
                .build()

        val clearOutdatedData =
            PeriodicWorkRequestBuilder<ClearOutdatedDataWorker>(1, DAYS)
                .setConstraints(constraints)
                .build()

        val riskyVenuesPollingCircuitBreakerWork =
            PeriodicWorkRequestBuilder<RiskyVenuesCircuitBreakerPollingWorker>(interval, timeUnit)
                .setConstraints(constraints)
                .build()

        val policy = if (keepPrevious) {
            KEEP
        } else {
            REPLACE
        }

        WorkManager.getInstance(context).apply {
            enqueueUniquePeriodicWork(
                PeriodicTask.DOWNLOAD_DIAGNOSIS_KEYS.workName,
                policy,
                downloadDiagnosisKeysRequest
            )
            enqueueUniquePeriodicWork(
                PeriodicTask.DOWNLOAD_RISKY_POST_CODES.workName,
                policy,
                downloadRiskyPostCodesRequest
            )
            enqueueUniquePeriodicWork(
                PeriodicTask.DOWNLOAD_RISKY_VENUES.workName,
                policy,
                downloadAndProcessRiskyVenus
            )
            enqueueUniquePeriodicWork(
                PeriodicTask.DOWNLOAD_VIROLOGY_TEST_RESULTS.workName,
                policy,
                downloadVirologyTestResults
            )
            enqueueUniquePeriodicWork(
                CLEAR_OUTDATED_DATA.workName,
                policy,
                clearOutdatedData
            )
            enqueueUniquePeriodicWork(
                RISKY_VENUE_POLLING.workName,
                policy,
                riskyVenuesPollingCircuitBreakerWork
            )
        }
    }

    @VisibleForTesting
    fun scheduleVirologyTestResultFetching() {
        val downloadVirologyTestResults =
            OneTimeWorkRequestBuilder<DownloadVirologyTestResultWorker>()
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            PeriodicTask.DOWNLOAD_VIROLOGY_TEST_RESULTS.workName,
            ExistingWorkPolicy.REPLACE,
            downloadVirologyTestResults
        )
    }

    fun scheduleExposureCircuitBreakerPolling(
        approvalToken: String,
        exposureDate: Long,
        interval: Long = 2,
        timeUnit: TimeUnit = HOURS
    ) {
        val inputData =
            Data.Builder()
                .putString(ExposurePollingCircuitBreakerWorker.APPROVAL_TOKEN, approvalToken)
                .putLong(ExposurePollingCircuitBreakerWorker.EXPOSURE_DATE, exposureDate)
                .build()

        val pollingExposureCircuitBreakerWork =
            OneTimeWorkRequestBuilder<ExposurePollingCircuitBreakerWorker>()
                .setBackoffCriteria(LINEAR, interval, timeUnit)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

        WorkManager.getInstance(context).enqueue(pollingExposureCircuitBreakerWork)
    }

    fun scheduleExposureCircuitBreakerInitial(
        token: String,
        interval: Long = 2,
        timeUnit: TimeUnit = HOURS
    ) {
        val inputData =
            Data.Builder()
                .putString(ExposureCircuitBreakerInitialWorker.EXPOSURE_TOKEN, token)
                .build()

        val initialExposureCircuitBreakerWork =
            OneTimeWorkRequestBuilder<ExposureCircuitBreakerInitialWorker>()
                .setBackoffCriteria(LINEAR, interval, timeUnit)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

        WorkManager.getInstance(context).enqueue(initialExposureCircuitBreakerWork)
    }

    fun scheduleKeysSubmission(
        payload: TemporaryExposureKeysPayload,
        interval: Long = 5,
        timeUnit: TimeUnit = MINUTES
    ) {
        val adapter = moshi.adapter(TemporaryExposureKeysPayload::class.java)
        val inputData =
            Data.Builder()
                .putString(
                    SubmitKeysWorker.TEMPORARY_EXPOSURE_KEYS_PAYLOAD,
                    adapter.toJson(payload)
                )
                .build()

        val work =
            OneTimeWorkRequestBuilder<SubmitKeysWorker>()
                .setBackoffCriteria(EXPONENTIAL, interval, timeUnit)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

        WorkManager.getInstance(context).enqueue(work)
    }
}

enum class PeriodicTask(val workName: String) {
    DOWNLOAD_DIAGNOSIS_KEYS("downloadDiagnosisKeysRequest"),
    DOWNLOAD_RISKY_POST_CODES("downloadRiskyPostCodesRequest"),
    DOWNLOAD_RISKY_VENUES("downloadAndProcessRiskyVenus"),
    DOWNLOAD_VIROLOGY_TEST_RESULTS("downloadVirologyTestResults"),
    CLEAR_OUTDATED_DATA("clearOutdatedData"),
    RISKY_VENUE_POLLING("RiskyVenuePolling")
}
