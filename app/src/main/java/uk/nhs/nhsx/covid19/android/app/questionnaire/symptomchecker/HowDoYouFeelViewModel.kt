package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelViewModel.NavigationTarget.Next
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelViewModel.NavigationTarget.Previous
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class HowDoYouFeelViewModel @AssistedInject constructor(
    @Assisted private val questions: SymptomsCheckerQuestions
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private var howDoYouFeelOption: BinaryRadioGroupOption? = null
    private var hasError: Boolean = false

    init {
        questions.howDoYouFeelSymptom?.isChecked?.let { isChecked ->
            howDoYouFeelOption = if (isChecked) YES else NO
            updateViewState()
        }
    }

    fun onHowDoYouFeelOptionChecked(option: BinaryRadioGroupOption?) {
        howDoYouFeelOption = option
        updateViewState()
    }

    fun onClickContinue() {
        when (howDoYouFeelOption) {
            YES -> {
                hasError = false
                navigateLiveData.postValue(
                    Next(
                        questions.copy(howDoYouFeelSymptom = HowDoYouFeelSymptom(isChecked = true))
                    )
                )
            }
            NO -> {
                hasError = false
                navigateLiveData.postValue(
                    Next(
                        questions.copy(howDoYouFeelSymptom = HowDoYouFeelSymptom(isChecked = false))
                    )
                )
            }
            null -> {
                hasError = true
                updateViewState()
            }
        }
    }

    private fun updateViewState() {
        viewModelScope.launch {
            viewStateLiveData.postValue(ViewState(howDoYouFeelOption, hasError))
        }
    }

    fun onBackPressed() {
        val backData = when (howDoYouFeelOption) {
            YES -> {
                    Previous(
                        questions.copy(howDoYouFeelSymptom = HowDoYouFeelSymptom(isChecked = true))
                    )
            }
            NO -> {
                    Previous(
                        questions.copy(howDoYouFeelSymptom = HowDoYouFeelSymptom(isChecked = false))
                    )
            }
            null -> Previous(questions)
        }
        navigateLiveData.postValue(backData)
    }

    data class ViewState(val howDoYouFeelSelection: BinaryRadioGroupOption?, val hasError: Boolean)

    sealed class NavigationTarget {
        data class Next(val symptomsCheckerQuestions: SymptomsCheckerQuestions) : NavigationTarget()
        data class Previous(val symptomsCheckerQuestions: SymptomsCheckerQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SymptomsCheckerQuestions,
        ): HowDoYouFeelViewModel
    }
}
