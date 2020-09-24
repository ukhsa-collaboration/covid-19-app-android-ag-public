package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import android.app.Notification
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import kotlin.test.assertEquals

class DownloadKeysWorkerTest : FieldInjectionUnitTest() {

    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>()
    private val downloadAndProcessKeysMock = mockk<DownloadAndProcessKeys>(relaxed = true)
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val notificationProviderMock = mockk<NotificationProvider>()

    private val testSubject = spyk(
        DownloadKeysWorker(context, workerParameters).apply {
            appAvailabilityProvider = appAvailabilityProviderMock
            downloadAndProcessKeys = downloadAndProcessKeysMock
            analyticsEventProcessor = analyticsEventProcessorMock
            notificationProvider = notificationProviderMock
        }
    )

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { testSubject.setForeground(any()) } returns Unit
    }

    @Test
    fun `app is not available returns retry`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns false

        val result = testSubject.doWork()

        coVerify(exactly = 0) { analyticsEventProcessorMock.track(AnalyticsEvent.BackgroundTaskCompletion) }
        coVerify(exactly = 0) { downloadAndProcessKeysMock.invoke() }
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `app is available calls downloadAndProcessKeys invoke & tracking`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        coEvery { downloadAndProcessKeysMock.invoke() } returns Result.Success(Unit)
        every { notificationProviderMock.getUpdatingDatabaseNotification() } returns mockk()

        testSubject.doWork()

        coVerify(exactly = 1) { analyticsEventProcessorMock.track(AnalyticsEvent.BackgroundTaskCompletion) }
        coVerify(exactly = 1) { downloadAndProcessKeysMock.invoke() }
    }

    @Test
    fun `uses foreground worker`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        coEvery { downloadAndProcessKeysMock.invoke() } returns Result.Success(Unit)
        val notificationMock = mockk<Notification>()
        every { notificationProviderMock.getUpdatingDatabaseNotification() } returns notificationMock

        testSubject.doWork()

        verify { notificationProviderMock.getUpdatingDatabaseNotification() }

        val slot = slot<ForegroundInfo>()
        coVerify(exactly = 1) { testSubject.setForeground(capture(slot)) }
        assertEquals(notificationMock, slot.captured.notification)
    }
}
