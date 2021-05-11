package uk.nhs.nhsx.covid19.android.app.state

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class IsolationExpirationViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock
) : ViewModel() {

    val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    fun checkState(isolationExpiryDateString: String) {
        runCatching {
            val isolationExpiryDate = LocalDate.parse(isolationExpiryDateString)
            val expired = LocalDate.now(clock).isEqualOrAfter(isolationExpiryDate)

            val isolationState = isolationStateMachine.readLogicalState()
            val showTemperatureNotice = isolationState is PossiblyIsolating &&
                isolationState.isActiveIndexCase(clock)
            viewState.postValue(ViewState(expired, isolationExpiryDate, showTemperatureNotice))
        }
    }

    fun acknowledgeIsolationExpiration() {
        isolationStateMachine.acknowledgeIsolationExpiration()
    }

    data class ViewState(val expired: Boolean, val expiryDate: LocalDate, val showTemperatureNotice: Boolean)
}
