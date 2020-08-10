package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import javax.inject.Inject

class RiskyVenuesCircuitBreakerPollingWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var riskyVenuesCircuitBreakerPolling: RiskyVenuesCircuitBreakerPolling

    @Inject
    lateinit var appAvailabilityProvider: AppAvailabilityProvider

    @Inject
    lateinit var analyticsEventProcessor: AnalyticsEventProcessor

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)

        if (!appAvailabilityProvider.isAppAvailable()) {
            return Result.retry()
        }

        val approvalToken = inputData.getString(APPROVAL_TOKEN)
        val venueId = inputData.getString(VENUE_ID)

        analyticsEventProcessor.track(BackgroundTaskCompletion)
        return if (approvalToken.isNullOrEmpty() || venueId.isNullOrEmpty()) Result.failure()
        else riskyVenuesCircuitBreakerPolling.doWork(approvalToken, venueId)
    }

    companion object {
        const val APPROVAL_TOKEN = "APPROVAL_TOKEN"
        const val VENUE_ID = "VENUE_ID"
    }
}
