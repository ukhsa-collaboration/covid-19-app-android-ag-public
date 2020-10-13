package uk.nhs.nhsx.covid19.android.app.exposure

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class ShareKeysInformationViewModel @Inject constructor(
    private val fetchTemporaryExposureKeys: FetchTemporaryExposureKeys
) : ViewModel() {

    var exposureNotificationWasInitiallyDisabled = false
    var handleSubmitKeyResolutionStarted = false

    private val fetchKeysLiveData = SingleLiveEvent<TemporaryExposureKeysFetchResult>()
    fun fetchKeysResult(): LiveData<TemporaryExposureKeysFetchResult> = fetchKeysLiveData

    fun fetchKeys() {
        viewModelScope.launch {
            val exposureKeysFetchResult = fetchTemporaryExposureKeys()
            fetchKeysLiveData.postValue(exposureKeysFetchResult)
        }
    }
}
