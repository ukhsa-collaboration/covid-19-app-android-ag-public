package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.IsolationResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationAgeLimitViewModel @Inject constructor(
    private val acknowledgeRiskyContact: AcknowledgeRiskyContact,
    private val optOutOfContactIsolation: OptOutOfContactIsolation,
    private val getAgeLimitBeforeEncounter: GetAgeLimitBeforeEncounter,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val navigationTargetLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    fun updateViewState() {
        viewModelScope.launch {
            val ageLimit = getAgeLimitBeforeEncounter()
            if (ageLimit != null) {
                val isActiveContactCaseOnly = !isolationStateMachine.readLogicalState().isActiveIndexCase(clock)
                viewStateLiveData.postValue(ViewState(ageLimit, showError, showSubtitle = isActiveContactCaseOnly))
            } else {
                Timber.e("Age limit calculation error")
                navigationTargetLiveData.postValue(Finish)
            }
        }
    }

    fun onAgeLimitOptionChanged(option: BinaryRadioGroupOption?) {
        ageLimitSelection = option
    }

    fun onClickContinue() {
        when (ageLimitSelection) {
            YES -> {
                showError = false
                navigationTargetLiveData.postValue(VaccinationStatus)
            }
            NO -> {
                showError = false
                acknowledgeRiskyContact()
                optOutOfContactIsolation()
                navigationTargetLiveData.postValue(IsolationResult)
            }
            null -> {
                showError = true
                updateViewState()
            }
        }
    }

    private var ageLimitSelection: BinaryRadioGroupOption? = null
    private var showError: Boolean = false

    data class ViewState(val date: LocalDate, val hasError: Boolean, val showSubtitle: Boolean)

    sealed class NavigationTarget {
        object Finish : NavigationTarget()
        object VaccinationStatus : NavigationTarget()
        object IsolationResult : NavigationTarget()
    }
}
