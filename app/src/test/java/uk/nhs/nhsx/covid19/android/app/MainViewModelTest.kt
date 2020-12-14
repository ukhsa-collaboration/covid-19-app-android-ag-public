package uk.nhs.nhsx.covid19.android.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
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
import uk.nhs.nhsx.covid19.android.app.util.viewutils.DeviceDetection

class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val deviceDetection = mockk<DeviceDetection>()

    private val exposureNotificationApi = mockk<ExposureNotificationApi>()

    private val mainViewState = mockk<Observer<MainViewModel.MainViewState>>(relaxed = true)

    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>(relaxed = true)

    private val policyUpdateProvider = mockk<PolicyUpdateProvider>()

    private val localAuthorityProvider = mockk<LocalAuthorityProvider>()

    private val batteryOptimizationRequired = mockk<BatteryOptimizationRequired>()

    private val postCodeProvider = mockk<PostCodeProvider>()

    private val localAuthorityPostCodeValidator = mockk<LocalAuthorityPostCodeValidator>()

    private val testSubject = MainViewModel(
        deviceDetection,
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

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `policy accepted and post code to local authority missing with local authority feature flag enabled`() = runBlocking {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        every { deviceDetection.isTablet() } returns false

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns true

        every { postCodeProvider.value } returns postCode

        coEvery { localAuthorityPostCodeValidator.validate(postCode) } returns Invalid

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.PostCodeToLocalAuthorityMissing) }
    }

    @Test
    fun `policy accepted and post code to local authority missing with local authority feature flag disabled`() = runBlocking {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        every { deviceDetection.isTablet() } returns false

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns true

        every { postCodeProvider.value } returns postCode

        coEvery { localAuthorityPostCodeValidator.validate(postCode) } returns Invalid

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.Completed) }
    }

    @Test
    fun `policy accepted and local authority missing with local authority feature flag enabled`() = runBlocking {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        every { deviceDetection.isTablet() } returns false

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
    fun `policy accepted and local authority missing with local authority feature flag disabled`() = runBlocking {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        every { deviceDetection.isTablet() } returns false

        coEvery { onboardingCompletedProvider.value } returns true

        every { policyUpdateProvider.isPolicyAccepted() } returns true

        every { localAuthorityProvider.value } returns null

        testSubject.viewState().observeForever(mainViewState)

        testSubject.start()

        verify { mainViewState.onChanged(MainViewModel.MainViewState.Completed) }
    }

    @Test
    fun `policy accepted and local authority present and battery optimization required`() = runBlocking {
        every { deviceDetection.isTablet() } returns false

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
        every { deviceDetection.isTablet() } returns false

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
