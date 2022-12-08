package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

class SubmitKeysProgressViewModel @AssistedInject constructor(
    private val submitTemporaryExposureKeys: SubmitTemporaryExposureKeys,
    private val keySharingInfoProvider: KeySharingInfoProvider,
    @Assisted private val exposureKeys: List<NHSTemporaryExposureKey>,
    @Assisted private val diagnosisKeySubmissionToken: String
) : ViewModel() {

    private val submitKeysLiveData: MutableLiveData<Lce<Unit>> = SingleLiveEvent()
    fun submitKeysResult(): LiveData<Lce<Unit>> = submitKeysLiveData

    fun submitKeys() {
        submitKeysLiveData.value = Lce.Loading
        viewModelScope.launch {
            val keySharingInfo = keySharingInfoProvider.keySharingInfo
            val viewState = if (keySharingInfo?.isSelfReporting == true) {
                when (val result = submitTemporaryExposureKeys(
                    exposureKeys, diagnosisKeySubmissionToken,
                    isPrivateJourney = true, keySharingInfo.testKitType.toString()
                )) {
                    is Failure -> Lce.Error(result.throwable)
                    is Success -> Lce.Success(Unit)
                }
            } else {
                when (val result = submitTemporaryExposureKeys(exposureKeys, diagnosisKeySubmissionToken)) {
                    is Failure -> Lce.Error(result.throwable)
                    is Success -> Lce.Success(Unit)
                }
            }
            submitKeysLiveData.postValue(viewState)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            exposureKeys: List<NHSTemporaryExposureKey>,
            diagnosisKeySubmissionToken: String
        ): SubmitKeysProgressViewModel
    }
}
