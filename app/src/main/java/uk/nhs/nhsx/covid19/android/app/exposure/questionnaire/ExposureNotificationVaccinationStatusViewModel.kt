package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Review
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.SelectionOutcome.Completion
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.SelectionOutcome.FollowupQuestion
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseEntry
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.AgeLimitQuestionType.IsAdult
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ReviewData
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationVaccinationStatusViewModel @Inject constructor(
    private val getLastDoseDateLimit: GetLastDoseDateLimit,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock,
    private val questionnaireFactory: QuestionnaireFactory
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private val isActiveContactCaseOnly: Boolean
        get() = isolationStateMachine.isActiveContactCaseOnly(clock)

    private var answers = listOf<Answer>()

    init {
        viewModelScope.launch {
            val questionnaire = questionnaireFactory.create()
            answers = listOf(Answer(questionnaire, answer = null))
            val lastDoseDateLimit = getLastDoseDateLimit()
            if (lastDoseDateLimit != null) {
                viewStateLiveData.postValue(
                    ViewState(
                        questions = questionsToShow(),
                        date = lastDoseDateLimit,
                        showSubtitle = isActiveContactCaseOnly
                    )
                )
            } else {
                Timber.e("Could not get last dose date limit")
                navigateLiveData.postValue(Finish)
            }
        }
    }

    private fun questionsToShow(): List<Question> {
        return answers.map { answer ->
            Question(answer.questionNode.questionType, answer.answer)
        }
    }

    fun onFullyVaccinatedOptionChanged(option: BinaryRadioGroupOption) {
        updateViewState(FullyVaccinated, option)
    }

    fun onLastDoseDateOptionChanged(option: BinaryRadioGroupOption) {
        updateViewState(DoseDate, option)
    }

    fun onClinicalTrialOptionChanged(option: BinaryRadioGroupOption) {
        updateViewState(ClinicalTrial, option)
    }

    fun onMedicallyExemptOptionChanged(option: BinaryRadioGroupOption) {
        updateViewState(MedicallyExempt, option)
    }

    private fun updateViewState(questionType: QuestionType, option: BinaryRadioGroupOption) {
        answers = updateAnswers(answers, questionType, option)
        viewStateLiveData.postValue(viewStateLiveData.value?.copy(questions = questionsToShow()))
    }

    private fun updateAnswers(
        answers: List<Answer>,
        questionType: QuestionType,
        answer: BinaryRadioGroupOption
    ): List<Answer> {
        val questionToUpdateIndex = answers.indexOfFirst { it.questionNode.questionType == questionType }
        val updatedAnswer = answers[questionToUpdateIndex].copy(answer = answer)
        return mutableListOf<Answer>().apply {
            addAll(answers.subList(0, questionToUpdateIndex))
            add(updatedAnswer)
            updatedAnswer.getNextAnswer()?.let { add(it) }
        }
    }

    fun onClickContinue() {
        viewModelScope.launch {
            val lastAnswer = answers.last()
            val questionnaireOutcome = lastAnswer.getOutcome()
            if (questionnaireOutcome != null) {
                val vaccinationStatusResponses = answers.map {
                    OptOutResponseEntry(
                        it.questionNode.questionType,
                        it.answer == BinaryRadioGroupOption.OPTION_1
                    )
                }

                viewStateLiveData.postValue(viewStateLiveData.value?.copy(showError = false))
                val reviewData = ReviewData(
                    questionnaireOutcome,
                    ageResponse = OptOutResponseEntry(IsAdult, true),
                    vaccinationStatusResponses = vaccinationStatusResponses
                )
                navigateToNextScreen(Review(reviewData))
            } else {
                viewStateLiveData.postValue(viewStateLiveData.value?.copy(showError = true))
            }
        }
    }

    private fun navigateToNextScreen(navigationTarget: NavigationTarget) {
        navigateLiveData.postValue(navigationTarget)
    }

    data class ViewState(
        val questions: List<Question> = listOf(),
        val showError: Boolean = false,
        val date: LocalDate,
        val showSubtitle: Boolean
    )

    data class Question(val questionType: VaccinationStatusQuestionType, val state: BinaryRadioGroupOption?)

    sealed class SelectionOutcome {
        data class FollowupQuestion(val questionNode: QuestionNode) : SelectionOutcome()
        data class Completion(val questionnaireOutcome: QuestionnaireOutcome) : SelectionOutcome()
    }

    data class QuestionNode(
        val questionType: VaccinationStatusQuestionType,
        val yes: SelectionOutcome,
        val no: SelectionOutcome
    ) {
        fun getFinalQuestionOrNull(answer: BinaryRadioGroupOption?): QuestionnaireOutcome? =
            if (answer == YES && yes is Completion) yes.questionnaireOutcome
            else if (answer == NO && no is Completion) no.questionnaireOutcome
            else null

        fun getNextAnswerOrNull(answer: BinaryRadioGroupOption?): Answer? =
            if (answer == YES && yes is FollowupQuestion) Answer(yes.questionNode, answer = null)
            else if (answer == NO && no is FollowupQuestion) Answer(no.questionNode, answer = null)
            else null
    }

    data class Answer(val questionNode: QuestionNode, val answer: BinaryRadioGroupOption?) {
        fun getOutcome(): QuestionnaireOutcome? = questionNode.getFinalQuestionOrNull(answer)
        fun getNextAnswer(): Answer? = questionNode.getNextAnswerOrNull(answer)
    }

    sealed class NavigationTarget {
        object Finish : NavigationTarget()
        data class Review(val reviewData: ReviewData) : NavigationTarget()
    }
}
