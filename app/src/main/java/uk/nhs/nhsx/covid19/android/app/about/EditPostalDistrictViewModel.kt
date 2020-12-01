package uk.nhs.nhsx.covid19.android.app.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.Success
import javax.inject.Inject

class EditPostalDistrictViewModel @Inject constructor(
    private val postCodeUpdater: PostCodeUpdater,
    private val updateAreaRisk: UpdateAreaRisk,
    private val localAuthorityPostCodeValidator: LocalAuthorityPostCodeValidator
) : ViewModel() {

    private val postCodeUpdateStateLiveData = MutableLiveData<PostCodeUpdateState>()
    fun postCodeUpdateState(): LiveData<PostCodeUpdateState> = postCodeUpdateStateLiveData

    private val postCodeValidationResultLiveData = MutableLiveData<LocalAuthorityPostCodeValidationResult>()
    fun postCodeValidationResult(): LiveData<LocalAuthorityPostCodeValidationResult> = postCodeValidationResultLiveData

    fun updatePostCode(postCode: String) {
        viewModelScope.launch {
            val updateResult: PostCodeUpdateState = postCodeUpdater.update(postCode)

            if (updateResult is Success) {
                updateAreaRisk.schedule()
            }

            postCodeUpdateStateLiveData.postValue(updateResult)
        }
    }

    fun validatePostCode(postCode: String) {
        viewModelScope.launch {
            postCodeValidationResultLiveData.postValue(localAuthorityPostCodeValidator.validate(postCode))
        }
    }
}
