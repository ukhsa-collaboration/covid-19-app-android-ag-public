package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import javax.inject.Inject

class RiskyVenuesCircuitBreakerInitialWorker(
    val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var riskyVenuesCircuitBreakerInitialWork: RiskyVenuesCircuitBreakerInitialWork

    @Inject
    lateinit var appAvailabilityProvider: AppAvailabilityProvider

    @Inject
    lateinit var analyticsEventProcessor: AnalyticsEventProcessor

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)

        if (!appAvailabilityProvider.isAppAvailable()) {
            return Result.retry()
        }

        analyticsEventProcessor.track(BackgroundTaskCompletion)
        return inputData.getString(VENUE_ID)?.let { venueId ->
            riskyVenuesCircuitBreakerInitialWork.doWork(venueId)
        } ?: Result.failure()
    }

    companion object {
        const val VENUE_ID = "VENUE_ID"
    }
}
