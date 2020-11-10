package uk.nhs.nhsx.covid19.android.app.availability

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.AppVersionNotSupported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.DeviceSdkIsNotSupported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.Supported
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityViewModel.AppAvailabilityState.UpdateAvailable
import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager.AvailableUpdateStatus.Available
import uk.nhs.nhsx.covid19.android.app.remote.data.MinimumAppVersion
import javax.inject.Inject

class AppAvailabilityViewModel @Inject constructor(
    private val appAvailabilityProvider: AppAvailabilityProvider,
    private val updateManager: UpdateManager
) : ViewModel() {
    private val appAvailabilityState = MutableLiveData<AppAvailabilityState>()
    fun appAvailabilityState(): LiveData<AppAvailabilityState> = appAvailabilityState

    fun checkAvailability(
        deviceSdkVersion: Int = Build.VERSION.SDK_INT,
        appVersionCode: Int = BuildConfig.VERSION_CODE
    ) {
        viewModelScope.launch {
            val appAvailability = appAvailabilityProvider.appAvailability

            if (appAvailability == null) {
                appAvailabilityState.postValue(Supported())
            } else {
                val minimumSDKVersion = appAvailability.minimumSdkVersion
                val minimumAppVersion = appAvailability.minimumAppVersion
                when {
                    minimumSDKVersion.value > deviceSdkVersion -> {
                        appAvailabilityState.postValue(
                            DeviceSdkIsNotSupported(
                                minimumSDKVersion.description.translate()
                            )
                        )
                    }
                    minimumAppVersion.value > appVersionCode -> {
                        checkIfThereIsAnUpdate(minimumAppVersion)
                    }
                    else -> {
                        appAvailabilityState.postValue(Supported())
                    }
                }
            }
        }
    }

    private suspend fun checkIfThereIsAnUpdate(minimumAppVersion: MinimumAppVersion) {
        when (val possibleUpdate = updateManager.getAvailableUpdateVersionCode()) {
            is Available -> {
                if (minimumAppVersion.value > possibleUpdate.versionCode)
                    appAvailabilityState.postValue(
                        AppVersionNotSupported(minimumAppVersion.description.translate())
                    )
                else
                    appAvailabilityState.postValue(UpdateAvailable(minimumAppVersion.description.translate()))
            }
            else -> appAvailabilityState.postValue(
                AppVersionNotSupported(
                    minimumAppVersion.description.translate()
                )
            )
        }
    }

    sealed class AppAvailabilityState {
        abstract val description: String

        data class DeviceSdkIsNotSupported(override val description: String) :
            AppAvailabilityState()

        data class AppVersionNotSupported(override val description: String) : AppAvailabilityState()
        data class UpdateAvailable(override val description: String) : AppAvailabilityState()
        data class Supported(override val description: String = "") : AppAvailabilityState()
    }
}
