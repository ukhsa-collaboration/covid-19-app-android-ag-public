package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitViewModel.NavigationTarget.Review
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationAgeLimitViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.getViewModelScopeOrDefault
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationAgeLimitViewModel(
    coroutineScopeOverride: CoroutineScope?,
    private val getAgeLimitBeforeEncounter: GetAgeLimitBeforeEncounter,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock
) : ViewModel() {

    @Inject constructor(
        getAgeLimitBeforeEncounter: GetAgeLimitBeforeEncounter,
        isolationStateMachine: IsolationStateMachine,
        clock: Clock
    ) : this(coroutineScopeOverride = null, getAgeLimitBeforeEncounter, isolationStateMachine, clock)

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val navigationTargetLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    private var ageLimitSelection: BinaryRadioGroupOption? = null
    private var showError: Boolean = false

    private val viewModelScope = getViewModelScopeOrDefault(coroutineScopeOverride)

    fun updateViewState() {
        viewModelScope.launch {
            val ageLimit = getAgeLimitBeforeEncounter()
            if (ageLimit != null) {
                val isActiveContactCaseOnly = !isolationStateMachine.readLogicalState().isActiveIndexCase(clock)
                viewStateLiveData.postValue(ViewState(ageLimitSelection, ageLimit, showError, showSubtitle = isActiveContactCaseOnly))
            } else {
                Timber.e("Age limit calculation error")
                navigationTargetLiveData.postValue(Finish)
            }
        }
    }

    fun onAgeLimitOptionChanged(option: BinaryRadioGroupOption?) {
        ageLimitSelection = option
        updateViewState()
    }

    fun onClickContinue() {
        when (ageLimitSelection) {
            YES -> {
                showError = false
                navigationTargetLiveData.postValue(VaccinationStatus)
            }
            NO -> {
                showError = false
                navigationTargetLiveData.postValue(Review)
            }
            null -> {
                showError = true
                updateViewState()
            }
        }
    }

    data class ViewState(val ageLimitSelection: BinaryRadioGroupOption?, val date: LocalDate, val hasError: Boolean, val showSubtitle: Boolean)

    sealed class NavigationTarget {
        object Finish : NavigationTarget()
        object VaccinationStatus : NavigationTarget()
        object Review : NavigationTarget()
    }
}
