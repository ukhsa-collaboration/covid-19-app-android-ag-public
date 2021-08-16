package uk.nhs.nhsx.covid19.android.app.common

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsViewModel.ActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class EnableExposureNotificationsViewModel @Inject constructor(
    exposureNotificationPermissionHelperFactory: ExposureNotificationPermissionHelper.Factory
) : ViewModel(), ExposureNotificationPermissionHelper.Callback {

    private val permissionRequestLiveData = SingleLiveEvent<(Activity) -> Unit>()
    fun permissionRequest(): LiveData<(Activity) -> Unit> = permissionRequestLiveData

    private val activationResultLiveData = SingleLiveEvent<ActivationResult>()
    fun activationResult(): LiveData<ActivationResult> = activationResultLiveData

    private val exposureNotificationPermissionHelper =
        exposureNotificationPermissionHelperFactory.create(this, viewModelScope)

    override fun onExposureNotificationsEnabled() {
        Timber.d("Exposure notifications successfully started")
        activationResultLiveData.postValue(Success)
    }

    override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
        permissionRequestLiveData.postValue(permissionRequest)
    }

    override fun onPermissionDenied() {
        Timber.d("Permission to start contact tracing denied")
    }

    override fun onError(error: Throwable) {
        Timber.e(error, "Could not start exposure notifications")
        val message = error.message ?: "Error starting contact tracing"
        activationResultLiveData.postValue(ActivationResult.Error(message))
    }

    fun onEnableExposureNotificationsClicked() {
        viewModelScope.launch {
            exposureNotificationPermissionHelper.startExposureNotifications()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        exposureNotificationPermissionHelper.onActivityResult(requestCode, resultCode)
    }

    sealed class ActivationResult {
        object Success : ActivationResult()
        data class Error(val message: String) : ActivationResult()
    }
}
