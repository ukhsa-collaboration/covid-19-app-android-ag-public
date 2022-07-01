package uk.nhs.nhsx.covid19.android.app.state

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class IsolationExpirationViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) : ViewModel() {

    val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    fun checkState(isolationExpiryDateString: String) {
        runCatching {
            viewModelScope.launch {
                val isolationExpiryDate = LocalDate.parse(isolationExpiryDateString)
                val expired = LocalDate.now(clock).isEqualOrAfter(isolationExpiryDate)
                val isolationState = isolationStateMachine.readLogicalState()
                val showTemperatureNotice = isolationState.remembersIndexCase()
                val country = localAuthorityPostCodeProvider.requirePostCodeDistrict()
                viewState.postValue(ViewState(expired, isolationExpiryDate, showTemperatureNotice, country))
            }
        }
    }

    fun acknowledgeIsolationExpiration() {
        isolationStateMachine.acknowledgeIsolationExpiration()
    }

    data class ViewState(
        val expired: Boolean,
        val expiryDate: LocalDate,
        val showTemperatureNotice: Boolean,
        var country: PostCodeDistrict
    )
}
