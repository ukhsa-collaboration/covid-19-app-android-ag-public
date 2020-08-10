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
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys

class ExposureStatusViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val exposureNotificationService = mockk<ExposureNotificationManager>()

    private val submitTemporaryExposureKeys = mockk<SubmitTemporaryExposureKeys>()

    private val testSubject =
        ExposureStatusViewModel(exposureNotificationService, submitTemporaryExposureKeys)

    private val activationResultObserver =
        mockk<Observer<ExposureNotificationActivationResult>>(relaxed = true)

    private val exposureNotificationsEnabledObserver = mockk<Observer<Boolean>>(relaxed = true)

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
    fun `exposure notification is enabled returns false`() = runBlocking {
        testSubject.exposureNotificationsEnabled()
            .observeForever(exposureNotificationsEnabledObserver)

        coEvery { exposureNotificationService.isEnabled() } returns false

        testSubject.checkExposureNotificationsEnabled()

        verify { exposureNotificationsEnabledObserver.onChanged(false) }
    }

    @Test
    fun `stop exposure notifications`() = runBlocking {

        testSubject.exposureNotificationsEnabled()
            .observeForever(exposureNotificationsEnabledObserver)

        coEvery { exposureNotificationService.isEnabled() } returns true
        coEvery { exposureNotificationService.stopExposureNotifications() } returns Unit

        testSubject.stopExposureNotifications()

        coVerify { exposureNotificationService.stopExposureNotifications() }
    }
}
