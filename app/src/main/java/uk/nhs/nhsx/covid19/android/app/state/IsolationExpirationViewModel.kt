package uk.nhs.nhsx.covid19.android.app.state

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
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

    lateinit var isolationExpiryDateString: String

    fun checkState(isolationExpiryDateString: String) {
        runCatching {
            this.isolationExpiryDateString = isolationExpiryDateString
            val isolationExpiryDate = LocalDate.parse(isolationExpiryDateString)
            val expired = LocalDate.now(clock).isEqualOrAfter(isolationExpiryDate)
            val isolationState = isolationStateMachine.readLogicalState()
            val showTemperatureNotice = isolationState.remembersIndexCase()
            isActiveOrPreviousIndexCase(expired, isolationExpiryDate, showTemperatureNotice)
        }
    }

    fun acknowledgeIsolationExpiration() {
        isolationStateMachine.acknowledgeIsolationExpiration()
    }

    private fun isActiveOrPreviousIndexCase(
        expired: Boolean,
        isolationExpiryDate: LocalDate,
        showTemperatureNotice: Boolean
    ) {
        var result = false
        viewModelScope.launch {
            if (localAuthorityPostCodeProvider.requirePostCodeDistrict() == ENGLAND)
                result = false
            else {
                val isActiveIndexCase = isolationStateMachine.readLogicalState().isActiveIndexCase(clock)
                val remembersIndexCase = isolationStateMachine.readLogicalState().remembersIndexCase()
                val remembersBothCases = isolationStateMachine.readLogicalState().remembersBothCases()

                result = when {
                    (isActiveIndexCase || (remembersIndexCase && !remembersBothCases)) -> true
                    (remembersBothCases) -> {
                        val indexCaseExpiryDate =
                            ((isolationStateMachine.readLogicalState() as PossiblyIsolating).indexInfo as IndexCase).expiryDate
                        indexCaseExpiryDate.isEqual(LocalDate.parse(isolationExpiryDateString))
                    }
                    else -> false
                }
            }
            viewState.postValue(ViewState(expired, isolationExpiryDate, showTemperatureNotice, result))
        }
    }

    data class ViewState(
        val expired: Boolean,
        val expiryDate: LocalDate,
        val showTemperatureNotice: Boolean,
        val isActiveOrPreviousIndexCase: Boolean
    )
}
