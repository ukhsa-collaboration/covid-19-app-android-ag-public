package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeValidator.LocalAuthorityPostCodeValidationResult.Valid
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeViewModel.NavigationTarget.LocalAuthority
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class PostCodeViewModel @Inject constructor(
    private val localAuthorityPostCodeValidator: LocalAuthorityPostCodeValidator
) : ViewModel() {

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

    private val postCodeValidationErrorLiveData = MutableLiveData<LocalAuthorityPostCodeValidationResult>()
    fun postCodeValidationError(): LiveData<LocalAuthorityPostCodeValidationResult> = postCodeValidationErrorLiveData

    fun validateMainPostCode(postCode: String) {
        viewModelScope.launch {
            val result = localAuthorityPostCodeValidator.validate(postCode)
            if (result is Valid)
                navigationTarget.postValue(LocalAuthority(postCode))
            postCodeValidationErrorLiveData.postValue(result)
        }
    }

    sealed class NavigationTarget {
        data class LocalAuthority(val postCode: String) : NavigationTarget()
    }
}
