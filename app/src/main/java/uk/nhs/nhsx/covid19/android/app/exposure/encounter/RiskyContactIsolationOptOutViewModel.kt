package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.exposure.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.NEW_ADVICE
import javax.inject.Inject

class RiskyContactIsolationOptOutViewModel @Inject constructor(
    private val optOutOfContactIsolation: OptOutOfContactIsolation,
    private val acknowledgeRiskyContact: AcknowledgeRiskyContact,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) : ViewModel() {

    private val localAuthorityPostCodeLiveData = MutableLiveData<PostCodeDistrict>()
    val localAuthorityPostCode: LiveData<PostCodeDistrict> = localAuthorityPostCodeLiveData

    fun acknowledgeAndOptOutContactIsolation() {
        optOutOfContactIsolation(reason = NEW_ADVICE)
        acknowledgeRiskyContact()
    }

    fun updateViewState() {
        viewModelScope.launch {
            localAuthorityPostCodeLiveData.postValue(localAuthorityPostCodeProvider.requirePostCodeDistrict())
        }
    }
}
