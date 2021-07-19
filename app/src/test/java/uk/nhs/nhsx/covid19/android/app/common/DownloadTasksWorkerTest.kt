package uk.nhs.nhsx.covid19.android.app.common

import android.app.Notification
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.jeroenmols.featureflag.framework.FeatureFlag.SUBMIT_ANALYTICS_VIA_ALARM_MANAGER
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalytics
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.availability.GetAvailabilityStatus
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWork
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.HasSuccessfullyProcessedNewExposureProvider
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadAndProcessKeys
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShowShareKeysReminderNotificationIfNeeded
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.DownloadAndProcessRiskyVenues
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWork
import uk.nhs.nhsx.covid19.android.app.status.localmessage.DownloadLocalMessagesWork
import uk.nhs.nhsx.covid19.android.app.testordering.DownloadVirologyTestResultWork
import uk.nhs.nhsx.covid19.android.app.util.crashreporting.ProcessRemoteServiceExceptionCrashReport
import kotlin.test.assertEquals

class DownloadTasksWorkerTest : FieldInjectionUnitTest() {

    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val getAvailabilityStatusMock = mockk<GetAvailabilityStatus>(relaxUnitFun = true)
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>(relaxUnitFun = true)
    private val analyticsEventProcessorMock = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val downloadVirologyTestResultWorkMock = mockk<DownloadVirologyTestResultWork>(relaxUnitFun = true)
    private val downloadRiskyPostCodesWorkMock = mockk<DownloadRiskyPostCodesWork>(relaxUnitFun = true)
    private val downloadLocalMessagesWorkMock = mockk<DownloadLocalMessagesWork>()
    private val downloadAndProcessRiskyVenuesMock = mockk<DownloadAndProcessRiskyVenues>(relaxUnitFun = true)
    private val downloadAndProcessKeysMock = mockk<DownloadAndProcessKeys>(relaxUnitFun = true)
    private val clearOutdatedDataAndUpdateIsolationConfigurationMock =
        mockk<ClearOutdatedDataAndUpdateIsolationConfiguration>(relaxUnitFun = true)
    private val exposureNotificationWorkMock = mockk<ExposureNotificationWork>(relaxUnitFun = true)
    private val notificationProviderMock = mockk<NotificationProvider>(relaxUnitFun = true)
    private val submitAnalyticsMock = mockk<SubmitAnalytics>(relaxUnitFun = true)
    private val onboardingCompletedProviderMock = mockk<OnboardingCompletedProvider>(relaxUnitFun = true)
    private val hasSuccessfullyProcessedNewExposureProviderMock =
        mockk<HasSuccessfullyProcessedNewExposureProvider>(relaxUnitFun = true)
    private val notificationMock = mockk<Notification>()
    private val showShareKeysReminderNotificationIfNeededMock = mockk<ShowShareKeysReminderNotificationIfNeeded>()
    private val processRemoteServiceExceptionCrashReportMock = mockk<ProcessRemoteServiceExceptionCrashReport>(relaxUnitFun = true)

    private var result: Result? = null

    private val testSubject = spyk(
        DownloadTasksWorker(context, workerParameters).apply {
            getAvailabilityStatus = getAvailabilityStatusMock
            appAvailabilityProvider = appAvailabilityProviderMock
            analyticsEventProcessor = analyticsEventProcessorMock
            downloadVirologyTestResultWork = downloadVirologyTestResultWorkMock
            downloadRiskyPostCodesWork = downloadRiskyPostCodesWorkMock
            downloadLocalMessagesWork = downloadLocalMessagesWorkMock
            downloadAndProcessRiskyVenues = downloadAndProcessRiskyVenuesMock
            downloadAndProcessKeys = downloadAndProcessKeysMock
            clearOutdatedDataAndUpdateIsolationConfiguration = clearOutdatedDataAndUpdateIsolationConfigurationMock
            exposureNotificationWork = exposureNotificationWorkMock
            notificationProvider = notificationProviderMock
            submitAnalytics = submitAnalyticsMock
            onboardingCompletedProvider = onboardingCompletedProviderMock
            hasSuccessfullyProcessedNewExposureProvider = hasSuccessfullyProcessedNewExposureProviderMock
            showShareKeysReminderNotificationIfNeeded = showShareKeysReminderNotificationIfNeededMock
            processRemoteServiceExceptionCrashReport = processRemoteServiceExceptionCrashReportMock
        }
    )

    @Before
    override fun setUp() {
        super.setUp()
        coEvery { getAvailabilityStatusMock.invoke() } returns mockk()
        coEvery { clearOutdatedDataAndUpdateIsolationConfigurationMock.invoke() } returns mockk()
        coEvery { exposureNotificationWorkMock.evaluateRisk() } returns mockk()
        coEvery { exposureNotificationWorkMock.handleUnprocessedRequests() } returns mockk()
        coEvery { downloadAndProcessKeysMock.invoke() } returns mockk()
        coEvery { downloadVirologyTestResultWorkMock.invoke() } returns mockk()
        coEvery { downloadRiskyPostCodesWorkMock.invoke() } returns mockk()
        coEvery { downloadLocalMessagesWorkMock.invoke() } returns mockk()
        coEvery { downloadAndProcessRiskyVenuesMock.invoke() } returns mockk()
        every { notificationProviderMock.getUpdatingDatabaseNotification() } returns notificationMock
        coEvery { testSubject.setForeground(any()) } returns Unit
        coEvery { submitAnalyticsMock.invoke(any()) } returns mockk()
        coEvery { submitAnalyticsMock.invoke(any()) } returns mockk()
        every { hasSuccessfullyProcessedNewExposureProviderMock.value } returns null
        coEvery { showShareKeysReminderNotificationIfNeededMock.invoke() } returns mockk()
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `app is not available returns failure`() = runBlocking {
        givenAppIsNotAvailable()
        givenOnboardingIsCompleted()

        whenDoingWork()

        thenBackgroundTaskCompletionEventIsNotTracked()
        thenWorkHasFailed()
    }

    @Test
    fun `onboarding not completed returns failure`() = runBlocking {
        givenAppIsAvailable()
        givenOnboardingIsNotCompleted()

        whenDoingWork()

        thenBackgroundTaskCompletionEventIsNotTracked()
        thenWorkHasFailed()
    }

    @Test
    fun `app is available calls cleanup, tracking and download tasks when last attempt to process new exposure was successful`() = runBlocking {
        givenFeatureSubmitAnalyticsViaAlarmManagerIsDisabled()
        givenAppIsAvailable()
        givenOnboardingIsCompleted()
        givenHasSuccessfullyProcessedNewExposure()

        whenDoingWork()

        thenBackgroundTaskCompletionEventIsTracked()
        thenUpdatingDatabaseNotificationIsProvided()
        thenAllTasksAreInvokedInTheRightOrder(shouldInvokeHandleNewExposure = false)
        thenWorkHasSucceeded()
    }

    @Test
    fun `app is available calls cleanup, tracking and download tasks when last attempt to process new exposure failed`() = runBlocking {
        givenFeatureSubmitAnalyticsViaAlarmManagerIsDisabled()
        givenAppIsAvailable()
        givenOnboardingIsCompleted()
        givenHasNotSuccessfullyProcessedNewExposure()

        whenDoingWork()

        thenBackgroundTaskCompletionEventIsTracked()
        thenUpdatingDatabaseNotificationIsProvided()
        thenAllTasksAreInvokedInTheRightOrder(shouldInvokeHandleNewExposure = true)
        thenWorkHasSucceeded()
    }

    @Test
    fun `worker does not send analytics when feature flag is enabled`() = runBlocking {
        givenFeatureSubmitAnalyticsViaAlarmManagerIsEnabled()
        givenAppIsAvailable()
        givenOnboardingIsCompleted()

        whenDoingWork()

        thenNoAnalyticsAreSubmitted()
        thenWorkHasSucceeded()
    }

    @Test
    fun `uses foreground worker`() = runBlocking {
        givenOnboardingIsCompleted()
        givenAppIsAvailable()

        whenDoingWork()

        thenUpdatingDatabaseNotificationIsProvided()
        thenForegroundIsStartedWithUpdatingDatabaseNotification()
    }

    private fun givenFeatureSubmitAnalyticsViaAlarmManagerIsEnabled() {
        FeatureFlagTestHelper.enableFeatureFlag(SUBMIT_ANALYTICS_VIA_ALARM_MANAGER)
    }

    private fun givenFeatureSubmitAnalyticsViaAlarmManagerIsDisabled() {
        FeatureFlagTestHelper.disableFeatureFlag(SUBMIT_ANALYTICS_VIA_ALARM_MANAGER)
    }

    private fun givenOnboardingIsCompleted() {
        coEvery { onboardingCompletedProviderMock.value } returns true
    }

    private fun givenOnboardingIsNotCompleted() {
        every { onboardingCompletedProviderMock.value } returns null
    }

    private fun givenAppIsAvailable() {
        every { appAvailabilityProviderMock.isAppAvailable() } returns true
    }

    private fun givenAppIsNotAvailable() {
        every { appAvailabilityProviderMock.isAppAvailable() } returns false
    }

    private fun givenHasSuccessfullyProcessedNewExposure() {
        every { hasSuccessfullyProcessedNewExposureProviderMock.value } returns true
    }

    private fun givenHasNotSuccessfullyProcessedNewExposure() {
        every { hasSuccessfullyProcessedNewExposureProviderMock.value } returns false
    }

    private suspend fun whenDoingWork() {
        result = testSubject.doWork()
    }

    private fun thenAllTasksAreInvokedInTheRightOrder(shouldInvokeHandleNewExposure: Boolean) {
        coVerifyOrder {
            clearOutdatedDataAndUpdateIsolationConfigurationMock()
            if (shouldInvokeHandleNewExposure) exposureNotificationWorkMock.evaluateRisk()
            exposureNotificationWorkMock.handleUnprocessedRequests()
            downloadAndProcessKeysMock()
            downloadVirologyTestResultWorkMock()
            downloadRiskyPostCodesWorkMock()
            downloadLocalMessagesWorkMock()
            downloadAndProcessRiskyVenuesMock()
            showShareKeysReminderNotificationIfNeededMock()
            submitAnalyticsMock(any())
            processRemoteServiceExceptionCrashReportMock()
        }
    }

    private fun thenNoAnalyticsAreSubmitted() {
        coVerify(exactly = 0) { submitAnalyticsMock(any()) }
    }

    private fun thenUpdatingDatabaseNotificationIsProvided() {
        verify { notificationProviderMock.getUpdatingDatabaseNotification() }
    }

    private fun thenBackgroundTaskCompletionEventIsTracked() {
        verify { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }
    }

    private fun thenBackgroundTaskCompletionEventIsNotTracked() {
        verify(exactly = 0) { analyticsEventProcessorMock.track(BackgroundTaskCompletion) }
    }

    private fun thenForegroundIsStartedWithUpdatingDatabaseNotification() {
        val slot = slot<ForegroundInfo>()
        coVerify { testSubject.setForeground(capture(slot)) }
        assertEquals(notificationMock, slot.captured.notification)
    }

    private fun thenWorkHasSucceeded() {
        assertEquals(Result.success(), result)
    }

    private fun thenWorkHasFailed() {
        assertEquals(Result.failure(), result)
    }
}
