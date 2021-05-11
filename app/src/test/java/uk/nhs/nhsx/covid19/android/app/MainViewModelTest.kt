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
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationRequired
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.PolicyUpdateProvider

class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val mainViewState = mockk<Observer<MainViewModel.MainViewState>>(relaxUnitFun = true)
    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>()
    private val policyUpdateProvider = mockk<PolicyUpdateProvider>()
    private val localAuthorityProvider = mockk<LocalAuthorityProvider>()
    private val batteryOptimizationRequired = mockk<BatteryOptimizationRequired>()
    private val postCodeProvider = mockk<PostCodeProvider>()
    private val localAuthorityPostCodeValidator = mockk<LocalAuthorityPostCodeValidator>()

    private val testSubject = MainViewModel(
        exposureNotificationApi,
        onboardingCompletedProvider,
        policyUpdateProvider,
        localAuthorityProvider,
        batteryOptimizationRequired,
        postCodeProvider,
        localAuthorityPostCodeValidator
    )

    private val postCode = "A1"

    @Before
    fun setUp() {
        coEvery { exposureNotificationApi.isAvailable() } returns true
        every { batteryOptimizationRequired() } returns false
    }

    @Test
    fun `policy accepted and post code to local authority missing`() = runBlocking {
        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns true

        every { postCodeProvider.value } returns postCode

        coEvery { localAuthorityPostCodeValidator.validate(postCode) } returns Invalid

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.PostCodeToLocalAuthorityMissing) }
    }

    @Test
    fun `policy accepted and local authority missing`() = runBlocking {

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns true

        every { postCodeProvider.value } returns postCode

        coEvery { localAuthorityPostCodeValidator.validate(postCode) } returns Valid(postCode, emptyList())

        every { localAuthorityProvider.value } returns null

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.LocalAuthorityMissing) }
    }

    @Test
    fun `policy accepted and local authority present and battery optimization required`() = runBlocking {

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns true

        every { postCodeProvider.value } returns postCode

        coEvery { localAuthorityPostCodeValidator.validate(postCode) } returns Valid(postCode, emptyList())

        every { localAuthorityProvider.value } returns "1"

        every { batteryOptimizationRequired() } returns true

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.BatteryOptimizationNotAcknowledged) }
    }

    @Test
    fun `policy accepted and local authority present`() = runBlocking {

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns true

        every { postCodeProvider.value } returns postCode

        coEvery { localAuthorityPostCodeValidator.validate(postCode) } returns Valid(postCode, emptyList())

        every { localAuthorityProvider.value } returns "1"

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.Completed) }
    }

    @Test
    fun `policy updated`() = runBlocking {

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns false

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.PolicyUpdated) }
    }

    @Test
    fun `exposure notifications not available`() = runBlocking {

        coEvery { exposureNotificationApi.isAvailable() } returns false

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.ExposureNotificationsNotAvailable) }
    }

    @Test
    fun `start onboarding`() = runBlocking {

        coEvery { exposureNotificationApi.isEnabled() } returns false

        coEvery { onboardingCompletedProvider.value } returns false

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.OnboardingStarted) }
    }

    @Test
    fun `when post code was entered and permissions not enabled user will be in OnboardingStarted state`() =
        runBlocking {

            coEvery { exposureNotificationApi.isEnabled() } returns false

            coEvery { onboardingCompletedProvider.value } returns false

            testSubject.viewState().observeForever(mainViewState)

            testSubject.start()

            verify { mainViewState.onChanged(MainViewModel.MainViewState.OnboardingStarted) }
        }
}
