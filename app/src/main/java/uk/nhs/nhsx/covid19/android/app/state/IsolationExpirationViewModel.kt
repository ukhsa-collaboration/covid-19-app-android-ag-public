package uk.nhs.nhsx.covid19.android.app.state

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class IsolationExpirationViewModel(
    private val clock: Clock,
    private val stateMachine: IsolationStateMachine
) : ViewModel() {

    @Inject
    constructor(stateMachine: IsolationStateMachine) : this(Clock.systemDefaultZone(), stateMachine)

    val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    fun checkState(isolationExpiryDateString: String) {
        runCatching {
            val isolationExpiryDate = LocalDate.parse(isolationExpiryDateString)
            val expiry = isolationExpiryDate.atStartOfDay(clock.zone).toInstant()
            val expired = Instant.now(clock).isAfter(expiry)

            val state = stateMachine.readState() as? Isolation
            val showTemperatureNotice = state?.indexCase != null
            viewState.postValue(ViewState(expired, isolationExpiryDate, showTemperatureNotice))
        }
    }

    data class ViewState(val expired: Boolean, val expiryDate: LocalDate, val showTemperatureNotice: Boolean)
}
