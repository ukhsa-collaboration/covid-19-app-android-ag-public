package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Cardinal
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.LoadQuestionnaire
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NonCardinal
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.YourSymptomsViewModel.NavigationTarget.Next
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2
import javax.inject.Inject

class YourSymptomsViewModel @Inject constructor(
    private val loadQuestionnaire: LoadQuestionnaire,
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<Lce<ViewState>>()
    fun viewState(): LiveData<Lce<ViewState>> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private lateinit var cardinal: Cardinal
    private lateinit var nonCardinal: NonCardinal
    private var nonCardinalOption: BinaryRadioGroupOption? = null
    private var cardinalOption: BinaryRadioGroupOption? = null
    private var hasDidNotCheckAnyQuestionError: Boolean = false
    private var hasDidNotCheckNonCardinalSymptomsError: Boolean = false
    private var hasDidNotCheckCardinalSymptomError: Boolean = false

    private var symptomsCheckerQuestions: SymptomsCheckerQuestions = SymptomsCheckerQuestions(null, null, null)

    fun loadQuestionnaire(questions: SymptomsCheckerQuestions? = null) {
        if (questions != null) {
            symptomsCheckerQuestions = questions
        }

        if (questions?.cardinalSymptom?.isChecked != null &&
            questions.nonCardinalSymptoms?.isChecked != null &&
            nonCardinalOption == null &&
            cardinalOption == null
        ) {
            nonCardinalOption = if (questions.nonCardinalSymptoms.isChecked) OPTION_1 else OPTION_2
            cardinalOption = if (questions.cardinalSymptom.isChecked) OPTION_1 else OPTION_2
        }

        if (viewStateLiveData.value is Lce.Success) {
            val status = viewStateLiveData.value!!.data!!.copy(
                nonCardinalSymptomsSelection = nonCardinalOption,
                cardinalSymptomSelection = cardinalOption
            )
            viewStateLiveData.postValue(Lce.Success(status))
            return
        }

        viewModelScope.launch {
            viewStateLiveData.postValue(Lce.Loading)

            when (val result = loadQuestionnaire.invoke()) {
                is Success -> {
                    cardinal = result.value.cardinal
                    nonCardinal = result.value.noncardinal
                    val state = ViewState(
                        cardinal,
                        nonCardinal,
                        nonCardinalOption,
                        cardinalOption,
                        hasDidNotCheckAnyQuestionError,
                        hasDidNotCheckNonCardinalSymptomsError,
                        hasDidNotCheckCardinalSymptomError
                    )
                    viewStateLiveData.postValue(Lce.Success(state))
                }
                is Failure -> viewStateLiveData.postValue(Lce.Error(result.throwable))
            }
        }
    }

    private fun updateViewState() {
        viewModelScope.launch {
            viewStateLiveData.postValue(
                Lce.Success(
                    ViewState(
                        cardinal, nonCardinal, nonCardinalOption, cardinalOption, hasDidNotCheckAnyQuestionError,
                        hasDidNotCheckNonCardinalSymptomsError, hasDidNotCheckCardinalSymptomError
                    )
                )
            )
        }
    }

    fun onNonCardinalOptionChecked(option: BinaryRadioGroupOption?) {
        nonCardinalOption = option
        updateViewState()
    }

    fun onCardinalOptionChecked(option: BinaryRadioGroupOption?) {
        cardinalOption = option
        updateViewState()
    }

    fun onClickContinue() {
        hasDidNotCheckNonCardinalSymptomsError = nonCardinalOption == null
        hasDidNotCheckCardinalSymptomError = cardinalOption == null
        hasDidNotCheckAnyQuestionError = (nonCardinalOption == null && cardinalOption == null)
        updateViewState()

        if (nonCardinalOption != null && cardinalOption != null) {
            navigateLiveData.postValue(
                Next(
                    symptomsCheckerQuestions.copy(
                        cardinalSymptom = CardinalSymptom(
                            isChecked = cardinalOption?.let {
                                binaryRadioGroupOptionToBoolean(
                                    it
                                )
                            },
                            title = cardinal.title
                        ),
                        nonCardinalSymptoms = NonCardinalSymptoms(
                            isChecked = nonCardinalOption?.let {
                                binaryRadioGroupOptionToBoolean(
                                    it
                                )
                            },
                            title = nonCardinal.title,
                            nonCardinalSymptomsText = nonCardinal.description
                        )
                    )
                )
            )
        }
    }

    private fun binaryRadioGroupOptionToBoolean(option: BinaryRadioGroupOption): Boolean {
        return when (option) {
            OPTION_1 -> true
            OPTION_2 -> false
        }
    }

    data class ViewState(
        val cardinal: Cardinal,
        val nonCardinal: NonCardinal,
        val nonCardinalSymptomsSelection: BinaryRadioGroupOption?,
        val cardinalSymptomSelection: BinaryRadioGroupOption?,
        val hasDidNotCheckAnyQuestionError: Boolean,
        val hasDidNotCheckNonCardinalSymptomsError: Boolean,
        val hasDidNotCheckCardinalSymptomsError: Boolean
    )

    sealed class NavigationTarget {
        data class Next(val symptomsCheckerQuestions: SymptomsCheckerQuestions) : NavigationTarget()
    }
}
