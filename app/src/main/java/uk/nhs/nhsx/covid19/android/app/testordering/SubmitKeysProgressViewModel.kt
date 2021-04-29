package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class SubmitKeysProgressViewModel @Inject constructor(
    private val submitTemporaryExposureKeys: SubmitTemporaryExposureKeys
) : ViewModel() {

    private val submitKeysLiveData: MutableLiveData<Result<Unit>> = SingleLiveEvent()
    fun submitKeysResult(): LiveData<Result<Unit>> = submitKeysLiveData

    fun submitKeys(
        exposureKeys: List<NHSTemporaryExposureKey>,
        diagnosisKeySubmissionToken: String
    ) {
        viewModelScope.launch {
            submitKeysLiveData.postValue(
                submitTemporaryExposureKeys(exposureKeys, diagnosisKeySubmissionToken)
            )
        }
    }
}
