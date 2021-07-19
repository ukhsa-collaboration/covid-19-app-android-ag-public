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

class ExposureStatusViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val exposureNotificationManager = mockk<ExposureNotificationManager>(relaxUnitFun = true)

    private val testSubject = ExposureStatusViewModel(exposureNotificationManager)

    private val activationResultObserver = mockk<Observer<ExposureNotificationActivationResult>>(relaxUnitFun = true)
    private val exposureNotificationsChangedObserver = mockk<Observer<Boolean>>(relaxUnitFun = true)
    private val exposureNotificationsEnabledObserver = mockk<Observer<Boolean>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        coEvery { exposureNotificationManager.isEnabled() } returns false
    }

    @Test
    fun `start exposure notifications observes success`() = runBlocking {

        testSubject.exposureNotificationActivationResult().observeForever(activationResultObserver)

        coEvery { exposureNotificationManager.startExposureNotifications() } returns ExposureNotificationActivationResult.Success

        testSubject.startExposureNotifications()

        verify { activationResultObserver.onChanged(ExposureNotificationActivationResult.Success) }
    }

    @Test
    fun `start exposure notifications resolution required`() = runBlocking {

        testSubject.exposureNotificationActivationResult().observeForever(activationResultObserver)

        val resolutionStatus = Status(CommonStatusCodes.RESOLUTION_REQUIRED)

        coEvery { exposureNotificationManager.startExposureNotifications() } returns ExposureNotificationActivationResult
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

        coEvery { exposureNotificationManager.startExposureNotifications() } returns ExposureNotificationActivationResult
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
    fun `exposure notification is changed returns true`() = runBlocking {
        testSubject.exposureNotificationsChanged()
            .observeForever(exposureNotificationsChangedObserver)

        coEvery { exposureNotificationManager.isEnabled() } returns true

        testSubject.checkExposureNotificationsChanged()

        verify { exposureNotificationsChangedObserver.onChanged(true) }
    }

    @Test
    fun `checking exposure notification is changed twice returns true only once`() = runBlocking {
        testSubject.exposureNotificationsChanged()
            .observeForever(exposureNotificationsChangedObserver)

        coEvery { exposureNotificationManager.isEnabled() } returns true

        testSubject.checkExposureNotificationsChanged()
        testSubject.checkExposureNotificationsChanged()

        verify(exactly = 1) { exposureNotificationsChangedObserver.onChanged(true) }
    }

    @Test
    fun `exposure notification is changed returns false`() = runBlocking {
        testSubject.exposureNotificationsChanged()
            .observeForever(exposureNotificationsChangedObserver)

        coEvery { exposureNotificationManager.isEnabled() } returns false

        testSubject.checkExposureNotificationsChanged()

        verify { exposureNotificationsChangedObserver.onChanged(false) }
    }

    @Test
    fun `exposure notification is enabled returns true`() = runBlocking {
        testSubject.exposureNotificationsEnabled()
            .observeForever(exposureNotificationsEnabledObserver)

        coEvery { exposureNotificationManager.isEnabled() } returns true

        testSubject.checkExposureNotificationsEnabled()

        verify { exposureNotificationsEnabledObserver.onChanged(true) }
    }

    @Test
    fun `checking exposure notification is enabled twice returns true twice`() = runBlocking {
        testSubject.exposureNotificationsEnabled()
            .observeForever(exposureNotificationsEnabledObserver)

        coEvery { exposureNotificationManager.isEnabled() } returns true

        testSubject.checkExposureNotificationsEnabled()
        testSubject.checkExposureNotificationsEnabled()

        verify(exactly = 2) { exposureNotificationsEnabledObserver.onChanged(true) }
    }

    @Test
    fun `exposure notification is enabled returns false`() = runBlocking {
        testSubject.exposureNotificationsEnabled()
            .observeForever(exposureNotificationsEnabledObserver)

        coEvery { exposureNotificationManager.isEnabled() } returns false

        testSubject.checkExposureNotificationsEnabled()

        verify { exposureNotificationsEnabledObserver.onChanged(false) }
    }

    @Test
    fun `stop exposure notifications`() = runBlocking {
        testSubject.exposureNotificationsChanged()
            .observeForever(exposureNotificationsChangedObserver)

        testSubject.stopExposureNotifications()

        coVerify { exposureNotificationManager.stopExposureNotifications() }
        verify { exposureNotificationsChangedObserver.onChanged(any()) }
    }
}
