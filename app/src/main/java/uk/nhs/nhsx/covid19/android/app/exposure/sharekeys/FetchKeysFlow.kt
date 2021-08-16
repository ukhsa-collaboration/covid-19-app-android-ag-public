package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey

class FetchKeysFlow @AssistedInject constructor(
    @Assisted private val callback: Callback,
    private val exposureNotificationManager: ExposureNotificationManager,
    exposureNotificationPermissionHelperFactory: ExposureNotificationPermissionHelper.Factory,
    fetchKeysHelperFactory: FetchKeysHelper.Factory,
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted private val keySharingInfo: KeySharingInfo
) {

    private var exposureNotificationWasInitiallyDisabled = false
    private var hasAlreadyFetchedKeys = false

    private val exposureNotificationPermissionHelper =
        exposureNotificationPermissionHelperFactory.create(
            object : ExposureNotificationPermissionHelper.Callback {
                override fun onExposureNotificationsEnabled() {
                    Timber.d("Exposure notifications successfully started")
                    fetchKeys()
                }

                override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
                    callback.onPermissionRequired(permissionRequest)
                }

                override fun onPermissionDenied() {
                    Timber.d("Permission to start contact tracing denied")
                }

                override fun onError(error: Throwable) {
                    Timber.e(error, "Could not start exposure notifications")
                }
            },
            coroutineScope
        )

    private val fetchKeysHelper =
        fetchKeysHelperFactory.create(
            object : FetchKeysHelper.Callback {
                override fun onSuccess(temporaryExposureKeys: List<NHSTemporaryExposureKey>, diagnosisKeySubmissionToken: String) {
                    disableExposureNotificationsAgainIfWasInitiallyDisabled()
                    hasAlreadyFetchedKeys = true
                    callback.onFetchKeysSuccess(temporaryExposureKeys, diagnosisKeySubmissionToken)
                }

                override fun onError(throwable: Throwable) {
                    val exposureNotificationsDisabled =
                        throwable is ApiException && throwable.statusCode == ConnectionResult.DEVELOPER_ERROR

                    if (exposureNotificationsDisabled) {
                        exposureNotificationPermissionHelper.startExposureNotifications()
                    } else {
                        Timber.e(throwable, "Failed to fetch exposure keys")
                        callback.onFetchKeysUnexpectedError()
                    }
                }

                override fun onPermissionRequired(permissionRequest: (Activity) -> Unit) {
                    callback.onPermissionRequired(permissionRequest)
                }

                override fun onPermissionDenied() {
                    disableExposureNotificationsAgainIfWasInitiallyDisabled()
                    callback.onFetchKeysPermissionDenied()
                }
            },
            coroutineScope,
            keySharingInfo
        )

    private fun disableExposureNotificationsAgainIfWasInitiallyDisabled() {
        coroutineScope.launch {
            if (exposureNotificationWasInitiallyDisabled) {
                exposureNotificationManager.stopExposureNotifications()
            }
        }
    }

    operator fun invoke() {
        coroutineScope.launch {
            if (exposureNotificationManager.isEnabled()) {
                fetchKeys()
            } else {
                exposureNotificationWasInitiallyDisabled = true
                exposureNotificationPermissionHelper.startExposureNotifications()
            }
        }
    }

    private fun fetchKeys() {
        if (hasAlreadyFetchedKeys) {
            return
        }
        coroutineScope.launch {
            fetchKeysHelper.fetchKeys()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        exposureNotificationPermissionHelper.onActivityResult(requestCode, resultCode)
        fetchKeysHelper.onActivityResult(requestCode, resultCode)
    }

    interface Callback {
        fun onFetchKeysSuccess(
            temporaryExposureKeys: List<NHSTemporaryExposureKey>,
            diagnosisKeySubmissionToken: String
        )

        fun onFetchKeysPermissionDenied()

        fun onFetchKeysUnexpectedError()
        fun onPermissionRequired(permissionRequest: (Activity) -> Unit)
    }

    @AssistedFactory
    interface Factory {
        fun create(callback: Callback, coroutineScope: CoroutineScope, keySharingInfo: KeySharingInfo): FetchKeysFlow
    }
}
