package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubViewModel.ViewState
import java.time.Duration

class ContactTracingHubViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val exposureNotificationManager = mockk<ExposureNotificationManager>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>()
    private val scheduleContactTracingActivationReminder =
        mockk<ScheduleContactTracingActivationReminder>(relaxUnitFun = true)
    private val exposureNotificationPermissionHelperFactory = mockk<ExposureNotificationPermissionHelper.Factory>()
    private val exposureNotificationPermissionHelper = mockk<ExposureNotificationPermissionHelper>(relaxUnitFun = true)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    private lateinit var testSubject: ContactTracingHubViewModel

    @Before
    fun setUp() {
        every {
            exposureNotificationPermissionHelperFactory.create(any(), any())
        } returns exposureNotificationPermissionHelper

        testSubject = ContactTracingHubViewModel(
            exposureNotificationManager,
            notificationProvider,
            scheduleContactTracingActivationReminder,
            exposureNotificationPermissionHelperFactory
        )

        testSubject.viewState.observeForever(viewStateObserver)
    }

    @Test
    fun `when onResume is called with exposure notifications enabled`() {
        coEvery { exposureNotificationManager.isEnabled() } returns true

        testSubject.onResume()

        val expectedViewState = ViewState(exposureNotificationEnabled = true, showReminderDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when onResume is called with exposure notifications disabled`() {
        coEvery { exposureNotificationManager.isEnabled() } returns false

        testSubject.onResume()

        val expectedViewState = ViewState(exposureNotificationEnabled = false, showReminderDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when onActivityResult is called then delegate invocation to EN permission helper`() {
        val expectedRequestCode = 1234
        val expectedResultCode = 5678

        testSubject.onActivityResult(expectedRequestCode, expectedResultCode)

        verify { exposureNotificationPermissionHelper.onActivityResult(expectedRequestCode, expectedResultCode) }
    }

    @Test
    fun `when exposure notifications are enabled and notification channel is disabled and toggle is clicked then stop exposure notifications`() {
        coEvery { exposureNotificationManager.isEnabled() } returns true andThen false
        every { notificationProvider.isChannelEnabled(any()) } returns false

        testSubject.onContactTracingToggleClicked()

        val expectedViewState = ViewState(exposureNotificationEnabled = false, showReminderDialog = false)

        coVerify { exposureNotificationManager.stopExposureNotifications() }
        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when exposure notifications are disabled and toggle is clicked then start exposure notifications`() {
        coEvery { exposureNotificationManager.isEnabled() } returns false
        every { notificationProvider.isChannelEnabled(any()) } returns false

        testSubject.onContactTracingToggleClicked()

        coVerify(exactly = 0) { exposureNotificationManager.stopExposureNotifications() }
        verify { exposureNotificationPermissionHelper.startExposureNotifications() }
    }

    @Test
    fun `when exposure notifications and notification channel are enabled and toggle is clicked then show reminder dialog`() {
        coEvery { exposureNotificationManager.isEnabled() } returns true
        every { notificationProvider.isChannelEnabled(any()) } returns true

        testSubject.onContactTracingToggleClicked()

        val expectedViewState = ViewState(exposureNotificationEnabled = true, showReminderDialog = true)

        coVerify(exactly = 0) { exposureNotificationManager.stopExposureNotifications() }
        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when reminder day selected then schedule activation reminder and stop exposure notifications`() {
        coEvery { exposureNotificationManager.isEnabled() } returns false

        val expectedDuration = Duration.ofHours(4)

        testSubject.onReminderDelaySelected(expectedDuration)

        val expectedViewState = ViewState(exposureNotificationEnabled = false, showReminderDialog = false)

        coVerifyOrder {
            scheduleContactTracingActivationReminder.invoke(expectedDuration)
            exposureNotificationManager.stopExposureNotifications()
            viewStateObserver.onChanged(expectedViewState)
        }
    }

    @Test
    fun `when exposure dialog is dismissed then update view state`() {
        coEvery { exposureNotificationManager.isEnabled() } returns true
        every { notificationProvider.isChannelEnabled(any()) } returns true

        testSubject.onContactTracingToggleClicked()

        testSubject.onExposureNotificationReminderDialogDismissed()

        val firstInvocationViewState = ViewState(exposureNotificationEnabled = true, showReminderDialog = true)
        val secondInvocationViewState = ViewState(exposureNotificationEnabled = true, showReminderDialog = false)

        verify { viewStateObserver.onChanged(firstInvocationViewState) }

        coVerify(exactly = 0) { exposureNotificationManager.stopExposureNotifications() }
        verifyOrder {
            viewStateObserver.onChanged(firstInvocationViewState)
            viewStateObserver.onChanged(secondInvocationViewState)
        }
    }

    @Test
    fun `when turn on contract tracing extra received is true start exposure notifications`() {
        coEvery { exposureNotificationManager.isEnabled() } returns true

        testSubject.onTurnOnContactTracingExtraReceived()

        verify { exposureNotificationPermissionHelper.startExposureNotifications() }

        verifyOrder {
            exposureNotificationPermissionHelper.startExposureNotifications()
            viewStateObserver.onChanged(
                ViewState(exposureNotificationEnabled = true, showReminderDialog = false)
            )
        }
    }
}
