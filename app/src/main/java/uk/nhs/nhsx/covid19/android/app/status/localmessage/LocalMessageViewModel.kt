package uk.nhs.nhsx.covid19.android.app.status.localmessage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.remote.data.NotificationMessage
import javax.inject.Inject

class LocalMessageViewModel @Inject constructor(
    private val getLocalMessageFromStorage: GetLocalMessageFromStorage
) : ViewModel() {

    private val viewState = MutableLiveData<NotificationMessage?>()
    fun viewState(): LiveData<NotificationMessage?> = viewState

    fun onCreate() {
        viewModelScope.launch {
            viewState.postValue(getLocalMessageFromStorage())
        }
    }
}
