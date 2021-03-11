package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult
import javax.inject.Inject

class PostCodeViewModel @Inject constructor(
    private val localAuthorityPostCodeValidator: LocalAuthorityPostCodeValidator
) : ViewModel() {

    private val postCodeValidationResultLiveData = MutableLiveData<LocalAuthorityPostCodeValidationResult>()
    fun postCodeValidationResult(): LiveData<LocalAuthorityPostCodeValidationResult> = postCodeValidationResultLiveData

    fun validateMainPostCode(postCode: String) {
        viewModelScope.launch {
            postCodeValidationResultLiveData.postValue(localAuthorityPostCodeValidator.validate(postCode))
        }
    }
}
