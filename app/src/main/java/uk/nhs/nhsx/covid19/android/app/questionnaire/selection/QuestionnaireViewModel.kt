package uk.nhs.nhsx.covid19.android.app.questionnaire.selection

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class QuestionnaireViewModel @Inject constructor(
    private val loadQuestionnaire: LoadQuestionnaire
) : ViewModel() {

    @VisibleForTesting
    var viewState = MutableLiveData<Lce<QuestionnaireState>>()
    fun viewState(): LiveData<Lce<QuestionnaireState>> = viewState

    private val navigateToReviewScreen = SingleLiveEvent<QuestionnaireState>()
    fun navigateToReviewScreen(): LiveData<QuestionnaireState> = navigateToReviewScreen

    fun loadQuestionnaire() {
        if (viewState.value is Lce.Success) {
            // Only load the questionnaire once
            return
        }

        viewModelScope.launch {
            viewState.postValue(Lce.Loading)

            when (val result = loadQuestionnaire.invoke()) {
                is Success -> {
                    val questions = createQuestions(result.value.symptoms)
                    val state = QuestionnaireState(
                        questions,
                        result.value.riskThreshold,
                        result.value.symptomsOnsetWindowDays,
                        showError = false,
                        showDialog = false
                    )
                    viewState.postValue(Lce.Success(state))
                }
                is Failure -> viewState.postValue(Lce.Error(result.throwable))
            }
        }
    }

    private fun createQuestions(symptoms: List<Symptom>): List<Question> {
        return symptoms.map { Question(it, isChecked = false) }
    }

    fun toggleQuestion(updatedQuestion: Question) {
        val currentViewState = viewState.value!!.data!!
        val toggledQuestion = updatedQuestion.copy(isChecked = !updatedQuestion.isChecked)
        val updatedQuestions = currentViewState.questions.map { question ->
            if (question.symptom == updatedQuestion.symptom) {
                toggledQuestion
            } else {
                question
            }
        }
        val updatedViewState =
            currentViewState.copy(questions = updatedQuestions, showError = false)
        viewState.postValue(Lce.Success(updatedViewState))
    }

    fun onButtonReviewSymptomsClicked() {
        val currentViewState = viewState.value!!.data!!
        if (currentViewState.questions.any { it.isChecked }) {
            navigateToReviewScreen.postValue(currentViewState.copy(showError = false))
        } else {
            viewState.postValue(Lce.Success(currentViewState.copy(showError = true)))
        }
    }

    fun onNoSymptomsClicked() {
        updateShowDialogState(true)
    }

    fun onDialogDismissed() {
        updateShowDialogState(false)
    }

    private fun updateShowDialogState(showDialog: Boolean) {
        val currentViewState = viewState.value!!.data!!
        val updatedViewState = currentViewState.copy(showDialog = showDialog)
        viewState.postValue(Lce.Success(updatedViewState))
    }
}

data class QuestionnaireState(
    val questions: List<Question>,
    val riskThreshold: Float,
    val symptomsOnsetWindowDays: Int,
    val showError: Boolean,
    val showDialog: Boolean
)
