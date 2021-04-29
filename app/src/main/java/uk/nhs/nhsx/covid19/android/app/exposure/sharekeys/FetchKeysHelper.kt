package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import android.app.Activity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey

class FetchKeysHelper @AssistedInject constructor(
    @Assisted private val callback: Callback,
    private val fetchTemporaryExposureKeys: FetchTemporaryExposureKeys,
    @Assisted private val coroutineScope: CoroutineScope,
    @Assisted private val keySharingInfo: KeySharingInfo,
) {

    fun fetchKeys() {
        coroutineScope.launch {
            val result = fetchTemporaryExposureKeys(keySharingInfo)
            Timber.d("Fetched keys: $result")
            when (result) {
                is Success -> callback.onSuccess(
                    result.temporaryExposureKeys,
                    keySharingInfo.diagnosisKeySubmissionToken
                )
                is Failure -> callback.onError(result.throwable)
                is ResolutionRequired -> {
                    val permissionRequestTrigger: (Activity) -> Unit = {
                        result.status.startResolutionForResult(it, REQUEST_CODE_SUBMIT_KEYS_PERMISSION)
                    }
                    callback.onPermissionRequired(permissionRequestTrigger)
                }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode == REQUEST_CODE_SUBMIT_KEYS_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                fetchKeys()
            } else {
                callback.onPermissionDenied()
            }
        }
    }

    interface Callback {
        fun onSuccess(temporaryExposureKeys: List<NHSTemporaryExposureKey>, diagnosisKeySubmissionToken: String)
        fun onError(throwable: Throwable)
        fun onPermissionRequired(permissionRequest: (Activity) -> Unit)
        fun onPermissionDenied()
    }

    @AssistedFactory
    interface Factory {
        fun create(callback: Callback, coroutineScope: CoroutineScope, keySharingInfo: KeySharingInfo?): FetchKeysHelper
    }

    companion object {
        const val REQUEST_CODE_SUBMIT_KEYS_PERMISSION = 1330
    }
}
