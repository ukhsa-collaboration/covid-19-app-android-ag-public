package uk.nhs.nhsx.covid19.android.app.onboarding.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState.Invalid
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState.Progress
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationCodeViewModel.AuthCodeViewState.Valid
import javax.inject.Inject

class AuthenticationCodeViewModel @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val authenticationCodeValidator: AuthenticationCodeValidator
) : ViewModel() {

    private val authCodeLiveData = MutableLiveData<AuthCodeViewState>()

    fun viewState(): LiveData<AuthCodeViewState> = authCodeLiveData

    fun validate(authCode: String) {
        val clearString = authCode.replace(AUTH_CODE_REGEX_FORMAT.toRegex(), "")

        viewModelScope.launch {
            authCodeLiveData.postValue(Progress)
            if (authenticationCodeValidator.validate(clearString)) {
                authenticationProvider.value = true
                authCodeLiveData.postValue(Valid)
            } else {
                authCodeLiveData.postValue(Invalid)
            }
        }
    }

    companion object {
        const val AUTH_CODE_REGEX_FORMAT = "[^a-z0-9]"
    }

    sealed class AuthCodeViewState {
        object Progress : AuthCodeViewState()
        object Valid : AuthCodeViewState()
        object Invalid : AuthCodeViewState()
    }
}
