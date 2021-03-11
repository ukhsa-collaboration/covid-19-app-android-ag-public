package uk.nhs.nhsx.covid19.android.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.BatteryOptimizationNotAcknowledged
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.Completed
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.ExposureNotificationsNotAvailable
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.LocalAuthorityMissing
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.OnboardingStarted
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.PolicyUpdated
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.PostCodeToLocalAuthorityMissing
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.TabletNotSupported
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationRequired
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Invalid
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.PolicyUpdateProvider
import uk.nhs.nhsx.covid19.android.app.util.defaultFalse
import uk.nhs.nhsx.covid19.android.app.util.viewutils.DeviceDetection
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val deviceDetection: DeviceDetection,
    private val exposureNotificationApi: ExposureNotificationApi,
    private val onboardingCompletedProvider: OnboardingCompletedProvider,
    private val policyUpdateProvider: PolicyUpdateProvider,
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val batteryOptimizationRequired: BatteryOptimizationRequired,
    private val postCodeProvider: PostCodeProvider,
    private val localAuthorityPostCodeValidator: LocalAuthorityPostCodeValidator
) : ViewModel() {

    private val mainViewStateLiveData = MutableLiveData<MainViewState>()

    fun viewState(): LiveData<MainViewState> = mainViewStateLiveData

    fun start() {
        viewModelScope.launch {

            val state = when {
                deviceDetection.isTablet() -> TabletNotSupported
                !exposureNotificationApi.isAvailable() -> ExposureNotificationsNotAvailable
                onboardingCompletedProvider.value.defaultFalse() && !policyUpdateProvider.isPolicyAccepted() -> PolicyUpdated
                onboardingCompletedProvider.value.defaultFalse() && policyUpdateProvider.isPolicyAccepted() ->
                    if (postCodeProvider.value != null &&
                        localAuthorityPostCodeValidator.validate(postCodeProvider.value!!) is Invalid
                    ) {
                        PostCodeToLocalAuthorityMissing
                    } else if (localAuthorityProvider.value == null) {
                        LocalAuthorityMissing
                    } else if (batteryOptimizationRequired()) {
                        BatteryOptimizationNotAcknowledged
                    } else {
                        Completed
                    }
                else -> OnboardingStarted
            }
            mainViewStateLiveData.postValue(state)
        }
    }

    sealed class MainViewState {
        object TabletNotSupported : MainViewState()
        object ExposureNotificationsNotAvailable : MainViewState()
        object OnboardingStarted : MainViewState()
        object PolicyUpdated : MainViewState()
        object PostCodeToLocalAuthorityMissing : MainViewState()
        object LocalAuthorityMissing : MainViewState()
        object BatteryOptimizationNotAcknowledged : MainViewState()
        object Completed : MainViewState()
    }
}
