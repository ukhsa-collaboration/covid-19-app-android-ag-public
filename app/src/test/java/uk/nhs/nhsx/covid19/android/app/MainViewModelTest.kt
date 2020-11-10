package uk.nhs.nhsx.covid19.android.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.PolicyUpdateProvider
import uk.nhs.nhsx.covid19.android.app.util.viewutils.DeviceDetection
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val deviceDetection = mockk<DeviceDetection>()

    private val exposureNotificationApi = mockk<ExposureNotificationApi>()

    private val mainViewState = mockk<Observer<MainViewModel.MainViewState>>(relaxed = true)

    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>(relaxed = true)

    private val policyUpdateProvider = mockk<PolicyUpdateProvider>()

    private val testSubject = MainViewModel(
        deviceDetection,
        exposureNotificationApi,
        onboardingCompletedProvider,
        policyUpdateProvider
    )

    @Before
    fun setUp() {
        coEvery { exposureNotificationApi.isAvailable() } returns true
    }

    @Test
    fun `policy accepted`() = runBlocking {
        every { deviceDetection.isTablet() } returns false

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns true

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.PolicyAccepted) }
    }

    @Test
    fun `policy updated`() = runBlocking {
        every { deviceDetection.isTablet() } returns false

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns false

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.PolicyUpdated) }
    }

    @Test
    fun `device not supported`() = runBlocking {
        every { deviceDetection.isTablet() } returns true

        coEvery { exposureNotificationApi.isEnabled() } returns false

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.TabletNotSupported) }
    }

    @Test
    fun `exposure notifications not available`() = runBlocking {
        every { deviceDetection.isTablet() } returns false

        coEvery { exposureNotificationApi.isAvailable() } returns false

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.ExposureNotificationsNotAvailable) }
    }

    @Test
    fun `start onboarding`() = runBlocking {
        every { deviceDetection.isTablet() } returns false

        coEvery { exposureNotificationApi.isEnabled() } returns false

        coEvery { onboardingCompletedProvider.value } returns false

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.OnboardingStarted) }
    }

    @Test
    fun `when post code was entered and permissions not enabled user will be in OnboardingStarted state`() =
        runBlocking {
            every { deviceDetection.isTablet() } returns false

            coEvery { exposureNotificationApi.isEnabled() } returns false

            coEvery { onboardingCompletedProvider.value } returns false

            testSubject.viewState().observeForever(mainViewState)

            testSubject.start()

            verify { mainViewState.onChanged(MainViewModel.MainViewState.OnboardingStarted) }
        }
}
