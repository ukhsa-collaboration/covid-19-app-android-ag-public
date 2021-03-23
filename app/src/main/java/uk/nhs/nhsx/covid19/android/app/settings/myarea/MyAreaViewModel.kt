package uk.nhs.nhsx.covid19.android.app.settings.myarea

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import javax.inject.Inject

class MyAreaViewModel @Inject constructor(
    private val postCodeProvider: PostCodeProvider,
    private val localAuthorityProvider: LocalAuthorityProvider,
    private val localAuthorityPostCodesLoader: LocalAuthorityPostCodesLoader,
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> = distinctUntilChanged(viewStateLiveData)

    fun onResume() {
        viewModelScope.launch {
            viewStateLiveData.postValue(
                ViewState(
                    postCode = postCodeProvider.value,
                    localAuthority = getLocalAuthority()
                )
            )
        }
    }

    private suspend fun getLocalAuthority(): String? {
        val localAuthorityId = localAuthorityProvider.value
        return if (localAuthorityId != null) {
            localAuthorityPostCodesLoader.load()?.localAuthorities?.get(localAuthorityId)?.name
        } else null
    }

    data class ViewState(
        val postCode: String?,
        val localAuthority: String?
    )
}
