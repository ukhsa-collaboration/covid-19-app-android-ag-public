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
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.QuestionnaireIsolationHandler
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomAdvice
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.AdviceScreen
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.ReviewSymptoms
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class QuestionnaireViewModel @Inject constructor(
    private val loadQuestionnaire: LoadQuestionnaire,
    private val questionnaireIsolationHandler: QuestionnaireIsolationHandler
) : ViewModel() {

    @VisibleForTesting
    var viewState = MutableLiveData<Lce<QuestionnaireState>>()
    fun viewState(): LiveData<Lce<QuestionnaireState>> = viewState

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

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
                        showDialog = false,
                        isSymptomaticSelfIsolationForWalesEnabled = result.value.isSymptomaticSelfIsolationForWalesEnabled
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
        val updatedViewState = currentViewState.copy(questions = updatedQuestions, showError = false)
        viewState.postValue(Lce.Success(updatedViewState))
    }

    fun onButtonReviewSymptomsClicked() {
        val currentViewState = viewState.value!!.data!!
        if (currentViewState.questions.any { it.isChecked }) {
            navigationTarget.postValue(
                ReviewSymptoms(
                    questions = currentViewState.questions,
                    riskThreshold = currentViewState.riskThreshold,
                    symptomsOnsetWindowDays = currentViewState.symptomsOnsetWindowDays,
                    isSymptomaticSelfIsolationForWalesEnabled = currentViewState.isSymptomaticSelfIsolationForWalesEnabled
                )
            )
        } else {
            viewState.postValue(Lce.Success(currentViewState.copy(showError = true)))
        }
    }

    fun onNoSymptomsClicked() {
        updateShowDialogState(showDialog = true)
    }

    fun onNoSymptomsConfirmed() {
        val noSymptomsAdvice = questionnaireIsolationHandler.computeAdvice(
            riskThreshold = Float.MAX_VALUE,
            selectedSymptoms = emptyList(),
            onsetDate = SelectedDate.NotStated,
            isSymptomaticSelfIsolationEnabled = true
        )
        navigationTarget.postValue(AdviceScreen(noSymptomsAdvice))
    }

    fun onNoSymptomsDialogDismissed() {
        updateShowDialogState(false)
    }

    private fun updateShowDialogState(showDialog: Boolean) {
        val currentViewState = viewState.value!!.data!!
        val updatedViewState = currentViewState.copy(showDialog = showDialog)
        viewState.postValue(Lce.Success(updatedViewState))
    }
}

sealed class NavigationTarget {
    data class ReviewSymptoms(
        val questions: List<Question>,
        val riskThreshold: Float,
        val symptomsOnsetWindowDays: Int,
        val isSymptomaticSelfIsolationForWalesEnabled: Boolean
    ) : NavigationTarget()

    data class AdviceScreen(val symptomAdvice: SymptomAdvice) : NavigationTarget()
}

data class QuestionnaireState(
    val questions: List<Question>,
    val riskThreshold: Float,
    val symptomsOnsetWindowDays: Int,
    val showError: Boolean,
    val showDialog: Boolean,
    val isSymptomaticSelfIsolationForWalesEnabled: Boolean
)
