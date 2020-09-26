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

    private val navigateToReviewScreen = SingleLiveEvent<List<Question>>()
    fun navigateToReviewScreen(): LiveData<List<Question>> = navigateToReviewScreen

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
                        showError = false
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
        val toggledQuestion = updatedQuestion.copy(isChecked = !updatedQuestion.isChecked)
        val questions = getQuestions() ?: return
        val riskThreshold = getRiskThreshold()
        val symptomsOnsetWindowDays = getSymptomsOnsetWindowDays()
        val updatedQuestions = questions.map { question ->
            if (question.symptom == updatedQuestion.symptom) {
                toggledQuestion
            } else {
                question
            }
        }
        val state = QuestionnaireState(updatedQuestions, riskThreshold, symptomsOnsetWindowDays, showError = false)
        viewState.postValue(Lce.Success(state))
    }

    fun onButtonReviewSymptomsClicked() {
        val questions = getQuestions() ?: return
        val riskThreshold = getRiskThreshold()
        val symptomsOnsetWindowDays = getSymptomsOnsetWindowDays()
        if (questions.any { it.isChecked }) {
            navigateToReviewScreen.postValue(questions)
        } else {
            val state = QuestionnaireState(questions, riskThreshold, symptomsOnsetWindowDays, showError = true)
            viewState.postValue(Lce.Success(state))
        }
    }

    fun onButtonCancelClicked(noSymptomsCallback: () -> Unit) {
        val questions = getQuestions() ?: return
        if (questions.any { it.isChecked }) {
            AlertDialog.Builder(this)
                .setTitle(R.string.questionnaire_discard_symptoms_dialog_title)
                .setMessage(R.string.questionnaire_discard_symptoms_dialog_message)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.remove) { _, _ ->
                    noSymptomsCallback()
                }
                .show()
        } else {
            noSymptomsCallback()
        }
        
    }

    private fun getQuestions(): List<Question>? {
        val result = viewState.value
        if (result is Lce.Success) {
            return result.data.questions
        }
        return null
    }

    fun getRiskThreshold(): Float {
        val result = viewState.value
        if (result is Lce.Success) {
            return result.data.riskThreshold
        }
        return 0.0F
    }

    fun getSymptomsOnsetWindowDays(): Int {
        val result = viewState.value
        if (result is Lce.Success) {
            return result.data.symptomsOnsetWindowDays
        }
        return 14
    }
}

data class QuestionnaireState(
    val questions: List<Question>,
    val riskThreshold: Float,
    val symptomsOnsetWindowDays: Int,
    val showError: Boolean
)
