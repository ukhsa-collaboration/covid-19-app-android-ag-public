package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import java.time.Instant
import javax.inject.Inject

class ExposurePollingCircuitBreakerWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var exposureCircuitBreakerPolling: ExposureCircuitBreakerPolling

    @Inject
    lateinit var appAvailabilityProvider: AppAvailabilityProvider

    @Inject
    lateinit var analyticsEventProcessor: AnalyticsEventProcessor

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)

        if (!appAvailabilityProvider.isAppAvailable()) {
            return Result.retry()
        }

        val token = inputData.getString(APPROVAL_TOKEN)!!
        val exposureDate = inputData.getLong(EXPOSURE_DATE, Instant.now().toEpochMilli())

        analyticsEventProcessor.track(BackgroundTaskCompletion)
        return exposureCircuitBreakerPolling.doWork(token, exposureDate)
    }

    companion object {
        const val EXPOSURE_DATE = "EXPOSURE_DATE"
        const val APPROVAL_TOKEN = "APPROVAL_TOKEN"
    }
}
