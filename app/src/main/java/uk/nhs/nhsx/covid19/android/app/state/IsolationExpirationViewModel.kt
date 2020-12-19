package uk.nhs.nhsx.covid19.android.app.state

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class IsolationExpirationViewModel @Inject constructor(
    private val stateMachine: IsolationStateMachine,
    private val clock: Clock
) : ViewModel() {

    val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    fun checkState(isolationExpiryDateString: String) {
        runCatching {
            val isolationExpiryDate = LocalDate.parse(isolationExpiryDateString)
            val expiry = isolationExpiryDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val expired = Instant.now(clock).isAfter(expiry)

            val state = stateMachine.readState() as? Isolation
            val showTemperatureNotice = state?.indexCase != null
            viewState.postValue(ViewState(expired, isolationExpiryDate, showTemperatureNotice))
        }
    }

    data class ViewState(val expired: Boolean, val expiryDate: LocalDate, val showTemperatureNotice: Boolean)
}
