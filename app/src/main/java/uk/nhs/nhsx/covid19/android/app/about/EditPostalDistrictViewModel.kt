package uk.nhs.nhsx.covid19.android.app.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeUpdater.PostCodeUpdateState.SUCCESS
import javax.inject.Inject

class EditPostalDistrictViewModel @Inject constructor(
    private val postCodeUpdater: PostCodeUpdater,
    private val updateAreaRisk: UpdateAreaRisk
) : ViewModel() {

    private val postCodeLiveData = MutableLiveData<PostCodeUpdateState>()
    fun viewState(): LiveData<PostCodeUpdateState> = postCodeLiveData

    fun updatePostCode(postCode: String) {
        viewModelScope.launch {
            val updateResult: PostCodeUpdateState = postCodeUpdater.update(postCode)

            if (updateResult == SUCCESS) {
                updateAreaRisk.schedule()
            }

            postCodeLiveData.postValue(updateResult)
        }
    }
}
