package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.IsolationResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationAgeLimitViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationAgeLimitViewModel @Inject constructor(
    private val acknowledgeRiskyContact: AcknowledgeRiskyContact,
    private val optOutOfContactIsolation: OptOutOfContactIsolation
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val navigationTargetLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    fun onAgeLimitOptionChanged(option: BinaryRadioGroupOption) {
        viewStateLiveData.postValue(
            ViewState(
                ageLimitSelection = option,
                showError = viewStateLiveData.value?.showError ?: false
            )
        )
    }

    fun onClickContinue() {
        val currentValue = viewStateLiveData.value ?: ViewState()
        when (currentValue.ageLimitSelection) {
            YES -> {
                viewStateLiveData.postValue(currentValue.copy(showError = false))
                navigationTargetLiveData.postValue(VaccinationStatus)
            }
            NO -> {
                acknowledgeRiskyContact()
                optOutOfContactIsolation()
                viewStateLiveData.postValue(currentValue.copy(showError = false))
                navigationTargetLiveData.postValue(IsolationResult)
            }
            null -> viewStateLiveData.postValue(ViewState(showError = true))
        }
    }

    data class ViewState(
        val ageLimitSelection: BinaryRadioGroupOption? = null,
        val showError: Boolean = false
    )

    sealed class NavigationTarget {
        object VaccinationStatus : NavigationTarget()
        object IsolationResult : NavigationTarget()
    }
}
