package uk.nhs.nhsx.covid19.android.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.ONBOARDING_AUTHENTICATION
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.MainViewModel.MainViewState.UserNotAuthenticated
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.util.DeviceDetection
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val deviceDetection: DeviceDetection,
    private val postCodePrefs: PostCodeProvider,
    private val authenticationProvider: AuthenticationProvider,
    private val exposureNotificationManager: ExposureNotificationManager
) : ViewModel() {

    private val mainViewStateLiveData = MutableLiveData<MainViewState>()

    fun viewState(): LiveData<MainViewState> = mainViewStateLiveData

    fun start() {
        viewModelScope.launch {
            val isOnboardingAuthenticationEnabled =
                RuntimeBehavior.isFeatureEnabled(ONBOARDING_AUTHENTICATION)

            val state: MainViewState = when {
                deviceDetection.isTablet() -> MainViewState.TabletNotSupported
                !isExposureNotificationsAvailable() -> MainViewState.ExposureNotificationsNotAvailable
                isOnboardingAuthenticationEnabled && !authenticationProvider.isAuthenticated() -> UserNotAuthenticated
                isExposureNotificationsEnabledAndMainPostCodeMissing() -> MainViewState.OnboardingPermissionsCompleted
                isOnboardingCompleted() -> MainViewState.OnboardingCompleted
                else -> MainViewState.OnboardingStarted
            }
            mainViewStateLiveData.postValue(state)
        }
    }

    private suspend fun isExposureNotificationsAvailable(): Boolean {
        val result = exposureNotificationManager.startExposureNotifications()
        return result !is ExposureNotificationActivationResult.Error
    }

    private suspend fun isExposureNotificationsEnabled() = exposureNotificationManager.isEnabled()

    private suspend fun isExposureNotificationsEnabledAndMainPostCodeMissing(): Boolean {
        val exposureNotificationsEnabled = isExposureNotificationsEnabled()
        Timber.d("exposureNotificationsEnabled $exposureNotificationsEnabled")
        val mainPostCodeEntered = isMainPostCodeEntered()
        Timber.d("mainPostCodeEntered $mainPostCodeEntered")
        return exposureNotificationsEnabled && !mainPostCodeEntered
    }

    private fun isOnboardingCompleted() = isMainPostCodeEntered()

    private fun isMainPostCodeEntered() = postCodePrefs.value?.isNotEmpty() ?: false

    sealed class MainViewState {

        object OnboardingCompleted : MainViewState()

        object TabletNotSupported : MainViewState()

        object ExposureNotificationsNotAvailable : MainViewState()

        object OnboardingStarted : MainViewState()

        object UserNotAuthenticated : MainViewState()

        object OnboardingPermissionsCompleted : MainViewState()
    }
}
