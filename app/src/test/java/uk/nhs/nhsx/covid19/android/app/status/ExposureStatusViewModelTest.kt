package uk.nhs.nhsx.covid19.android.app.status

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExposureStatusViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val exposureNotificationService = mockk<ExposureNotificationManager>(relaxed = true)

    private val exposureNotificationReminderAlarmController =
        mockk<ExposureNotificationReminderAlarmController>(relaxed = true)

    private val resumeContactTracingNotificationTimeProvider =
        mockk<ResumeContactTracingNotificationTimeProvider>(relaxed = true)

    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = ExposureStatusViewModel(
        exposureNotificationService,
        exposureNotificationReminderAlarmController,
        resumeContactTracingNotificationTimeProvider,
        fixedClock
    )

    private val activationResultObserver =
        mockk<Observer<ExposureNotificationActivationResult>>(relaxed = true)

    private val exposureNotificationsChangedObserver = mockk<Observer<Boolean>>(relaxed = true)
    private val exposureNotificationsEnabledObserver = mockk<Observer<Boolean>>(relaxed = true)
    private val exposureNotificationReminderRequestObserver = mockk<Observer<Void>>(relaxed = true)

    @Before
    fun setUp() {
        coEvery { exposureNotificationService.isEnabled() } returns false
    }

    @Test
    fun `start exposure notifications observes success`() = runBlocking {

        testSubject.exposureNotificationActivationResult().observeForever(activationResultObserver)

        coEvery { exposureNotificationService.startExposureNotifications() } returns ExposureNotificationActivationResult.Success

        testSubject.startExposureNotifications()

        verify { activationResultObserver.onChanged(ExposureNotificationActivationResult.Success) }
        verify { exposureNotificationReminderAlarmController.cancel() }
    }

    @Test
    fun `start exposure notifications resolution required`() = runBlocking {

        testSubject.exposureNotificationActivationResult().observeForever(activationResultObserver)

        val resolutionStatus = Status(CommonStatusCodes.RESOLUTION_REQUIRED)

        coEvery { exposureNotificationService.startExposureNotifications() } returns ExposureNotificationActivationResult
            .ResolutionRequired(
                resolutionStatus
            )

        testSubject.startExposureNotifications()

        verify {
            activationResultObserver.onChanged(
                ExposureNotificationActivationResult.ResolutionRequired(
                    resolutionStatus
                )
            )
        }
        verify(exactly = 0) { exposureNotificationReminderAlarmController.cancel() }
    }

    @Test
    fun `start exposure notifications returns error`() = runBlocking {

        testSubject.exposureNotificationActivationResult().observeForever(activationResultObserver)

        val testException = Exception()

        coEvery { exposureNotificationService.startExposureNotifications() } returns ExposureNotificationActivationResult
            .Error(
                testException
            )

        testSubject.startExposureNotifications()

        verify {
            activationResultObserver.onChanged(
                ExposureNotificationActivationResult.Error(
                    testException
                )
            )
        }
        verify(exactly = 0) { exposureNotificationReminderAlarmController.cancel() }
    }

    @Test
    fun `exposure notification is changed returns true`() = runBlocking {
        testSubject.exposureNotificationsChanged()
            .observeForever(exposureNotificationsChangedObserver)

        coEvery { exposureNotificationService.isEnabled() } returns true

        testSubject.checkExposureNotificationsChanged()

        verify { exposureNotificationsChangedObserver.onChanged(true) }
    }

    @Test
    fun `checking exposure notification is changed twice returns true only once`() = runBlocking {
        testSubject.exposureNotificationsChanged()
            .observeForever(exposureNotificationsChangedObserver)

        coEvery { exposureNotificationService.isEnabled() } returns true

        testSubject.checkExposureNotificationsChanged()
        testSubject.checkExposureNotificationsChanged()

        verify(exactly = 1) { exposureNotificationsChangedObserver.onChanged(true) }
    }

    @Test
    fun `exposure notification is changed returns false`() = runBlocking {
        testSubject.exposureNotificationsChanged()
            .observeForever(exposureNotificationsChangedObserver)

        coEvery { exposureNotificationService.isEnabled() } returns false

        testSubject.checkExposureNotificationsChanged()

        verify { exposureNotificationsChangedObserver.onChanged(false) }
    }

    @Test
    fun `exposure notification is enabled returns true`() = runBlocking {
        testSubject.exposureNotificationsEnabled()
            .observeForever(exposureNotificationsEnabledObserver)

        coEvery { exposureNotificationService.isEnabled() } returns true

        testSubject.checkExposureNotificationsEnabled()

        verify { exposureNotificationsEnabledObserver.onChanged(true) }
    }

    @Test
    fun `checking exposure notification is enabled twice returns true twice`() = runBlocking {
        testSubject.exposureNotificationsEnabled()
            .observeForever(exposureNotificationsEnabledObserver)

        coEvery { exposureNotificationService.isEnabled() } returns true

        testSubject.checkExposureNotificationsEnabled()
        testSubject.checkExposureNotificationsEnabled()

        verify(exactly = 2) { exposureNotificationsEnabledObserver.onChanged(true) }
    }

    @Test
    fun `exposure notification is enabled returns false`() = runBlocking {
        testSubject.exposureNotificationsEnabled()
            .observeForever(exposureNotificationsEnabledObserver)

        coEvery { exposureNotificationService.isEnabled() } returns false

        testSubject.checkExposureNotificationsEnabled()

        verify { exposureNotificationsEnabledObserver.onChanged(false) }
    }

    @Test
    fun `stop exposure notifications`() = runBlocking {
        testSubject.exposureNotificationsChanged()
            .observeForever(exposureNotificationsChangedObserver)

        testSubject.stopExposureNotifications()

        coVerify { exposureNotificationService.stopExposureNotifications() }
        verify { exposureNotificationsChangedObserver.onChanged(any()) }
    }
}
