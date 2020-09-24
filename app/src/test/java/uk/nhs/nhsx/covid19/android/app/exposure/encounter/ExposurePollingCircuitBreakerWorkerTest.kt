package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposurePollingCircuitBreakerWorker.Companion.APPROVAL_TOKEN
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposurePollingCircuitBreakerWorker.Companion.EXPOSURE_DATE
import java.time.Instant
import kotlin.test.assertEquals

class ExposurePollingCircuitBreakerWorkerTest : FieldInjectionUnitTest() {
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val exposureCircuitBreakerPollingMock = mockk<ExposureCircuitBreakerPolling>()
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>()
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val testSubject = ExposurePollingCircuitBreakerWorker(context, workerParameters).apply {
        exposureCircuitBreakerPolling = exposureCircuitBreakerPollingMock
        appAvailabilityProvider = appAvailabilityProviderMock
        analyticsEventProcessor = analyticsEventProcessorMock
    }

    @Test
    fun `app is not available returns failure`() = runBlocking {
        val token = "token"
        val exposureDate = Instant.now().toEpochMilli()

        every { appAvailabilityProviderMock.isAppAvailable() } returns false
        every { testSubject.inputData.getString(APPROVAL_TOKEN) } returns token
        every { testSubject.inputData.getLong(EXPOSURE_DATE, exposureDate) } returns exposureDate

        val result = testSubject.doWork()

        coVerify(exactly = 0) { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }
        coVerify(exactly = 0) { exposureCircuitBreakerPollingMock.doWork(token, exposureDate) }
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `app is available calls exposureCircuitBreakerPolling doWork & tracking`() = runBlocking {
        val token = "token"
        val exposureDate = Instant.now().toEpochMilli()

        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        every { testSubject.inputData.getString(APPROVAL_TOKEN) } returns token
        every { testSubject.inputData.getLong(EXPOSURE_DATE, any()) } returns exposureDate
        coEvery { exposureCircuitBreakerPollingMock.doWork(token, exposureDate) } returns Result.success()

        testSubject.doWork()

        coVerify(exactly = 1) { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }
        coVerify(exactly = 1) { exposureCircuitBreakerPollingMock.doWork(token, exposureDate) }
    }
}
