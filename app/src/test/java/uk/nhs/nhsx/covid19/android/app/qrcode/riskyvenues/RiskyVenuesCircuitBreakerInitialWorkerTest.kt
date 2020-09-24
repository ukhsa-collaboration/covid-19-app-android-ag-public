package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider

class RiskyVenuesCircuitBreakerInitialWorkerTest : FieldInjectionUnitTest() {

    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>()
    private val riskyVenuesCircuitBreakerInitialWorkMock = mockk<RiskyVenuesCircuitBreakerInitialWork>(relaxed = true)
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val testSubject = RiskyVenuesCircuitBreakerInitialWorker(context, workerParameters).apply {
        appAvailabilityProvider = appAvailabilityProviderMock
        riskyVenuesCircuitBreakerInitialWork = riskyVenuesCircuitBreakerInitialWorkMock
        analyticsEventProcessor = analyticsEventProcessorMock
    }

    @Test
    fun `app is not available returns failure`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns false

        val result = testSubject.doWork()

        coVerify(exactly = 0) { analyticsEventProcessorMock.track(AnalyticsEvent.BackgroundTaskCompletion) }
        coVerify(exactly = 0) { riskyVenuesCircuitBreakerInitialWorkMock.doWork(any()) }
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `app is available and venueIds not empty calls downloadAndProcessKeys invoke & tracking`() = runBlocking {
        val venueIds = arrayOf("1234", "5678")

        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        every { testSubject.inputData.getStringArray(RiskyVenuesCircuitBreakerTasks.VENUE_IDS) } returns venueIds
        coEvery { riskyVenuesCircuitBreakerInitialWorkMock.doWork(venueIds.toList()) } returns ListenableWorker.Result.success()

        testSubject.doWork()

        coVerify(exactly = 1) { analyticsEventProcessorMock.track(AnalyticsEvent.BackgroundTaskCompletion) }
        coVerify(exactly = 1) { riskyVenuesCircuitBreakerInitialWorkMock.doWork(venueIds.toList()) }
    }

    @Test
    fun `app is available and venueIds empty calls tracking but returns failure`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        every { testSubject.inputData.getStringArray(RiskyVenuesCircuitBreakerTasks.VENUE_IDS) } returns null

        testSubject.doWork()

        coVerify(exactly = 1) { analyticsEventProcessorMock.track(AnalyticsEvent.BackgroundTaskCompletion) }
        coVerify(exactly = 0) { riskyVenuesCircuitBreakerInitialWorkMock.doWork(any()) }
    }
}
