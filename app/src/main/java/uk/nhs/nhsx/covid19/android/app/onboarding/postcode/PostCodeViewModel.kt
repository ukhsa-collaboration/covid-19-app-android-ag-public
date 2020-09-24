package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class PostCodeViewModel @Inject constructor(
    private val postCodeUpdater: PostCodeUpdater
) : ViewModel() {

    private val postCodeLiveData = SingleLiveEvent<PostCodeUpdateState>()
    fun viewState(): LiveData<PostCodeUpdateState> = postCodeLiveData

    fun updateMainPostCode(postCode: String) {
        viewModelScope.launch {
            val updateResult: PostCodeUpdateState = postCodeUpdater.update(postCode)

            postCodeLiveData.postValue(updateResult)
        }
    }
}
