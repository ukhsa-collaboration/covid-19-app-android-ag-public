package uk.nhs.nhsx.covid19.android.app.common

import android.app.Notification
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalytics
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.availability.GetAvailabilityStatus
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWork
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadAndProcessKeys
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWork
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import kotlin.test.assertEquals

class DownloadTasksWorkerTest : FieldInjectionUnitTest() {

    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val getAvailabilityStatusMock = mockk<GetAvailabilityStatus>(relaxed = true)
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>(relaxed = true)
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val downloadVirologyTestResultWorkMock =
        mockk<DownloadVirologyTestResultWork>(relaxed = true)
    private val downloadRiskyPostCodesWorkMock = mockk<DownloadRiskyPostCodesWork>(relaxed = true)
    private val downloadAndProcessRiskyVenuesMock =
        mockk<DownloadAndProcessRiskyVenues>(relaxed = true)
    private val downloadAndProcessKeysMock = mockk<DownloadAndProcessKeys>(relaxed = true)
    private val clearOutdatedDataAndUpdateIsolationConfigurationMock = mockk<ClearOutdatedDataAndUpdateIsolationConfiguration>(relaxed = true)
    private val exposureNotificationWorkMock = mockk<ExposureNotificationWork>(relaxed = true)
    private val notificationProviderMock = mockk<NotificationProvider>(relaxed = true)
    private val submitAnalyticsMock = mockk<SubmitAnalytics>(relaxed = true)
    private val onboardingCompletedProviderMock = mockk<OnboardingCompletedProvider>(relaxed = true)

    private val testSubject = spyk(
        DownloadTasksWorker(context, workerParameters).apply {
            getAvailabilityStatus = getAvailabilityStatusMock
            appAvailabilityProvider = appAvailabilityProviderMock
            analyticsEventProcessor = analyticsEventProcessorMock
            downloadVirologyTestResultWork = downloadVirologyTestResultWorkMock
            downloadRiskyPostCodesWork = downloadRiskyPostCodesWorkMock
            downloadAndProcessRiskyVenues = downloadAndProcessRiskyVenuesMock
            downloadAndProcessKeys = downloadAndProcessKeysMock
            clearOutdatedDataAndUpdateIsolationConfiguration = clearOutdatedDataAndUpdateIsolationConfigurationMock
            exposureNotificationWork = exposureNotificationWorkMock
            notificationProvider = notificationProviderMock
            submitAnalytics = submitAnalyticsMock
            onboardingCompletedProvider = onboardingCompletedProviderMock
        }
    )

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { testSubject.setForeground(any()) } returns Unit
        coEvery { onboardingCompletedProviderMock.value } returns true
    }

    @Test
    fun `app is not available returns failure`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns false

        val result = testSubject.doWork()

        coVerify(exactly = 0) { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `onboarding not completed returns failure`() = runBlocking {
        every { onboardingCompletedProviderMock.value } returns null

        val result = testSubject.doWork()

        coVerify(exactly = 0) { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `app is available calls cleanup, tracking and download tasks`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns true

        val result = testSubject.doWork()

        coVerify { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }
        verify { notificationProviderMock.getUpdatingDatabaseNotification() }
        coVerifyOrder {
            clearOutdatedDataAndUpdateIsolationConfigurationMock()
            exposureNotificationWorkMock.handleMatchesFound()
            downloadAndProcessKeysMock()
            downloadVirologyTestResultWorkMock()
            downloadRiskyPostCodesWorkMock()
            downloadAndProcessRiskyVenuesMock()
        }

        assertEquals(Result.success(), result)
    }

    @Test
    fun `uses foreground worker`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        coEvery { downloadAndProcessKeysMock.invoke() } returns uk.nhs.nhsx.covid19.android.app.common.Result.Success(
            Unit
        )
        val notificationMock = mockk<Notification>()
        every { notificationProviderMock.getUpdatingDatabaseNotification() } returns notificationMock

        testSubject.doWork()

        verify { notificationProviderMock.getUpdatingDatabaseNotification() }

        val slot = slot<ForegroundInfo>()
        coVerify { testSubject.setForeground(capture(slot)) }
        assertEquals(notificationMock, slot.captured.notification)
    }
}
