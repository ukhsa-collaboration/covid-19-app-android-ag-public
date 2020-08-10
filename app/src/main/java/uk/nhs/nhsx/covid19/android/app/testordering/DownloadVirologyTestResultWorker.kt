package uk.nhs.nhsx.covid19.android.app.testordering

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import javax.inject.Inject

class DownloadVirologyTestResultWorker(
    val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var downloadVirologyTestResultWork: DownloadVirologyTestResultWork

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

        return downloadVirologyTestResultWork()
    }
}
