package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import kotlin.test.assertEquals

class DownloadAndProcessRiskyVenuesWorkerTest : FieldInjectionUnitTest() {

    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>()
    private val downloadAndProcessRiskyVenuesMock = mockk<DownloadAndProcessRiskyVenues>(relaxed = true)
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val testSubject = DownloadAndProcessRiskyVenuesWorker(context, workerParameters).apply {
        appAvailabilityProvider = appAvailabilityProviderMock
        downloadAndProcessRiskyVenues = downloadAndProcessRiskyVenuesMock
        analyticsEventProcessor = analyticsEventProcessorMock
    }

    @Test
    fun `app is not available returns failure`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns false

        val result = testSubject.doWork()

        coVerify(exactly = 0) { analyticsEventProcessorMock.track(AnalyticsEvent.BackgroundTaskCompletion) }
        coVerify(exactly = 0) { downloadAndProcessRiskyVenuesMock.invoke() }
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `app is available calls downloadAndProcessRiskyVenues invoke & tracking`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        coEvery { downloadAndProcessRiskyVenuesMock.invoke() } returns uk.nhs.nhsx.covid19.android.app.common.Result.Success(Unit)

        testSubject.doWork()

        coVerify(exactly = 1) { analyticsEventProcessorMock.track(AnalyticsEvent.BackgroundTaskCompletion) }
        coVerify(exactly = 1) { downloadAndProcessRiskyVenuesMock.invoke() }
    }
}
