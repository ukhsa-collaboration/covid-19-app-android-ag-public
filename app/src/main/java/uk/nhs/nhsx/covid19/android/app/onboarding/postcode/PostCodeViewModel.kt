package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class PostCodeViewModel @Inject constructor(
    private val postCodeUpdater: PostCodeUpdater,
    private val localAuthorityPostCodeValidator: LocalAuthorityPostCodeValidator
) : ViewModel() {

    private val postCodeLiveData = SingleLiveEvent<PostCodeUpdateState>()
    fun viewState(): LiveData<PostCodeUpdateState> = postCodeLiveData

    private val postCodeValidationResultLiveData = MutableLiveData<LocalAuthorityPostCodeValidationResult>()
    fun postCodeValidationResult(): LiveData<LocalAuthorityPostCodeValidationResult> = postCodeValidationResultLiveData

    fun updateMainPostCode(postCode: String) {
        viewModelScope.launch {
            val updateResult: PostCodeUpdateState = postCodeUpdater.update(postCode)

            postCodeLiveData.postValue(updateResult)
        }
    }

    fun validateMainPostCode(postCode: String) {
        viewModelScope.launch {
            postCodeValidationResultLiveData.postValue(localAuthorityPostCodeValidator.validate(postCode))
        }
    }
}
