package uk.nhs.nhsx.covid19.android.app.status

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class ExposureStatusViewModel @Inject constructor(
    private val exposureNotificationManager: ExposureNotificationManager,
    private val submitTemporaryExposureKeys: SubmitTemporaryExposureKeys
) : ViewModel() {

    private val exposureNotificationActivationResult =
        SingleLiveEvent<ExposureNotificationActivationResult>()

    fun exposureNotificationActivationResult(): SingleLiveEvent<ExposureNotificationActivationResult> =
        exposureNotificationActivationResult

    private val exposureNotificationsChangedLiveData = MutableLiveData<Boolean>()

    fun exposureNotificationsChanged(): LiveData<Boolean> =
        distinctUntilChanged(exposureNotificationsChangedLiveData)

    private val exposureNotificationsEnabledLiveData = MutableLiveData<Boolean>()

    fun exposureNotificationsEnabled(): LiveData<Boolean> = exposureNotificationsEnabledLiveData

    val submitKeyLiveData = MutableLiveData<SubmitResult>()

    fun checkExposureNotificationsChanged() {
        viewModelScope.launch {
            exposureNotificationsChangedLiveData.postValue(exposureNotificationManager.isEnabled())
        }
    }

    fun checkExposureNotificationsEnabled() {
        viewModelScope.launch {
            exposureNotificationsEnabledLiveData.postValue(exposureNotificationManager.isEnabled())
        }
    }

    fun startExposureNotifications() {
        viewModelScope.launch {
            if (!exposureNotificationManager.isEnabled()) {
                val startResult = exposureNotificationManager.startExposureNotifications()
                exposureNotificationActivationResult.postValue(startResult)
                checkExposureNotificationsChanged()
            } else {
                exposureNotificationActivationResult.postValue(Success)
            }
        }
    }

    fun submitKeys() {
        viewModelScope.launch {
            submitKeyLiveData.postValue(submitTemporaryExposureKeys(null))
        }
    }

    fun stopExposureNotifications() {
        viewModelScope.launch {
            exposureNotificationManager.stopExposureNotifications()
            checkExposureNotificationsChanged()
        }
    }

    companion object {
        const val REQUEST_CODE_SUBMIT_KEYS_PERMISSION = 1338
    }
}
