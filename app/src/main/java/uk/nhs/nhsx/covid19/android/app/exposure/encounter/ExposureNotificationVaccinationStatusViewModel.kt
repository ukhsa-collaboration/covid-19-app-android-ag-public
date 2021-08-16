package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Isolating
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import java.time.LocalDate
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationVaccinationStatusViewModel @Inject constructor(
    private val acknowledgeRiskyContact: AcknowledgeRiskyContact,
    private val optOutOfContactIsolation: OptOutOfContactIsolation,
    private val getLastDoseDateLimit: GetLastDoseDateLimit
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    fun onAllDosesOptionChanged(option: BinaryRadioGroupOption) {
        viewStateLiveData.postValue(
            currentValue().copy(
                allDosesSelection = option,
                doseDateSelection = null
            )
        )
    }

    fun onDoseDateOptionChanged(option: BinaryRadioGroupOption) {
        viewStateLiveData.postValue(
            currentValue().copy(doseDateSelection = option)
        )
    }

    fun onClickContinue() {
        val currentValue = currentValue()
        if (hasAnsweredRequisiteNumberOfQuestions()) {
            acknowledgeRiskyContact()
            if (currentValue.hasAnsweredFullyVaccinated())
                optOutOfContactIsolation()
            navigateToNextScreen()
        } else {
            viewStateLiveData.postValue(currentValue.copy(showError = true))
        }
    }

    fun lastDoseDateLimit(): LocalDate = getLastDoseDateLimit()

    private fun hasAnsweredRequisiteNumberOfQuestions() = with(currentValue()) {
        allDosesSelection == NO || doseDateSelection != null
    }

    private fun navigateToNextScreen() {
        val navigationTarget =
            if (currentValue().hasAnsweredFullyVaccinated()) FullyVaccinated
            else Isolating
        navigateLiveData.postValue(navigationTarget)
    }

    private fun currentValue(): ViewState {
        return viewStateLiveData.value ?: ViewState()
    }

    data class ViewState(
        val allDosesSelection: BinaryRadioGroupOption? = null,
        val doseDateSelection: BinaryRadioGroupOption? = null,
        val showError: Boolean = false
    ) {
        fun hasAnsweredFullyVaccinated() = allDosesSelection == YES && doseDateSelection == YES
    }

    sealed class NavigationTarget {
        object FullyVaccinated : NavigationTarget()
        object Isolating : NavigationTarget()
    }
}
