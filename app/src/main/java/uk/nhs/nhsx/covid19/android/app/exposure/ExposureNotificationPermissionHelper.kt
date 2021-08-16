package uk.nhs.nhsx.covid19.android.app.exposure

import android.app.Activity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Error
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Success

class ExposureNotificationPermissionHelper @AssistedInject constructor(
    @Assisted private val callback: Callback,
    private val exposureNotificationManager: ExposureNotificationManager,
    @Assisted private val coroutineScope: CoroutineScope,
) {

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION) {
            if (resultCode == Activity.RESULT_OK) {
                startExposureNotifications()
            } else {
                callback.onPermissionDenied()
            }
        }
    }

    fun startExposureNotifications() {
        coroutineScope.launch {
            val startResult = if (exposureNotificationManager.isEnabled()) {
                Success
            } else {
                exposureNotificationManager.startExposureNotifications()
            }
            when (startResult) {
                Success -> callback.onExposureNotificationsEnabled()
                is ResolutionRequired -> {
                    val permissionRequestTrigger: (Activity) -> Unit = {
                        startResult.status.startResolutionForResult(it, REQUEST_CODE_START_EXPOSURE_NOTIFICATION)
                    }
                    callback.onPermissionRequired(permissionRequestTrigger)
                }
                is Error -> callback.onError(startResult.exception)
            }
        }
    }

    interface Callback {
        fun onExposureNotificationsEnabled()
        fun onPermissionRequired(permissionRequest: (Activity) -> Unit)
        fun onPermissionDenied()
        fun onError(error: Throwable)
    }

    @AssistedFactory
    interface Factory {
        fun create(callback: Callback, coroutineScope: CoroutineScope): ExposureNotificationPermissionHelper
    }

    companion object {
        const val REQUEST_CODE_START_EXPOSURE_NOTIFICATION = 1331
    }
}
