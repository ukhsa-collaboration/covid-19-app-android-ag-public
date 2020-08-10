package uk.nhs.nhsx.covid19.android.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.Status
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.util.DeviceDetection

class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val deviceDetection = mockk<DeviceDetection>()

    private val postCodeProvider = mockk<PostCodeProvider>()

    private val authCodeProvider = mockk<AuthenticationProvider>()

    private val exposureNotificationManager = mockk<ExposureNotificationManager>()

    private val mainViewState = mockk<Observer<MainViewModel.MainViewState>>(relaxed = true)

    private val testSubject = MainViewModel(
        deviceDetection,
        postCodeProvider,
        authCodeProvider,
        exposureNotificationManager
    )

    @Before
    fun setUp() {
        val status = mockk<Status>()
        coEvery { exposureNotificationManager.startExposureNotifications() } returns ResolutionRequired(status)
    }

    @Test
    fun `onboarding completed`() = runBlocking {
        every { deviceDetection.isTablet() } returns false

        every { authCodeProvider.isAuthenticated() } returns true

        coEvery { exposureNotificationManager.isEnabled() } returns true

        every { postCodeProvider.value } returns "CM1"

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.OnboardingCompleted) }
    }

    @Test
    fun `device not supported`() = runBlocking {
        every { deviceDetection.isTablet() } returns true

        every { authCodeProvider.isAuthenticated() } returns true

        coEvery { exposureNotificationManager.isEnabled() } returns false

        every { postCodeProvider.value } returns null

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.TabletNotSupported) }
    }

    @Test
    fun `not authenticated`() = runBlocking {
        every { deviceDetection.isTablet() } returns false

        every { authCodeProvider.isAuthenticated() } returns false

        coEvery { exposureNotificationManager.isEnabled() } returns false

        every { postCodeProvider.value } returns null

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.UserNotAuthenticated) }
    }

    @Test
    fun `start onboarding`() = runBlocking {
        every { deviceDetection.isTablet() } returns false

        every { authCodeProvider.isAuthenticated() } returns true

        coEvery { exposureNotificationManager.isEnabled() } returns false

        every { postCodeProvider.value } returns null

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.OnboardingStarted) }
    }
}
