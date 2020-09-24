package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import kotlin.test.assertEquals

class ExposureCircuitBreakerInitialWorkerTest : FieldInjectionUnitTest() {
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>()
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val handleExposureNotificationMock = mockk<HandleExposureNotification>(relaxed = true)

    private val testSubject = ExposureCircuitBreakerInitialWorker(context, workerParameters).apply {
        appAvailabilityProvider = appAvailabilityProviderMock
        analyticsEventProcessor = analyticsEventProcessorMock
        handleExposureNotification = handleExposureNotificationMock
    }

    @Test
    fun `app is not available returns retry`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns false

        val result = testSubject.doWork()

        coVerify(exactly = 0) { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }
        coVerify(exactly = 0) { handleExposureNotificationMock.doWork("token") }
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `app is available calls handleExposureNotification doWork & tracking`() = runBlocking {
        val token = "token"

        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        every { testSubject.inputData.getString(ExposureCircuitBreakerInitialWorker.EXPOSURE_TOKEN) } returns token

        testSubject.doWork()

        coVerify(exactly = 1) { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }
        coVerify(exactly = 1) { handleExposureNotificationMock.doWork(token) }
    }
}
