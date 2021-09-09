package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Isolating
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.QuestionType.CLINICAL_TRIAL
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.QuestionType.DOSE_DATE
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.QuestionType.FULLY_VACCINATED
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.QuestionType.MEDICALLY_EXEMPT
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.SelectionOutcome.FollowupQuestion
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationVaccinationStatusViewModel.SelectionOutcome.Navigation
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationVaccinationStatusViewModel @Inject constructor(
    private val acknowledgeRiskyContact: AcknowledgeRiskyContact,
    private val optOutOfContactIsolation: OptOutOfContactIsolation,
    private val getLastDoseDateLimit: GetLastDoseDateLimit,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private val englishQuestionnaire = QuestionNode(
        FULLY_VACCINATED,
        yes = FollowupQuestion(
            QuestionNode(
                DOSE_DATE,
                yes = Navigation(FullyVaccinated),
                no = FollowupQuestion(
                    QuestionNode(
                        CLINICAL_TRIAL,
                        yes = Navigation(FullyVaccinated),
                        no = FollowupQuestion(
                            QuestionNode(
                                MEDICALLY_EXEMPT,
                                yes = Navigation(MedicallyExempt),
                                no = Navigation(Isolating)
                            ),
                        )
                    )
                )
            )
        ),
        no = FollowupQuestion(
            QuestionNode(
                MEDICALLY_EXEMPT,
                yes = Navigation(MedicallyExempt),
                no = FollowupQuestion(
                    QuestionNode(
                        CLINICAL_TRIAL,
                        yes = Navigation(FullyVaccinated),
                        no = Navigation(Isolating)
                    )
                )
            )
        )
    )

    private val welshQuestionnaire = QuestionNode(
        FULLY_VACCINATED,
        yes = FollowupQuestion(
            QuestionNode(
                DOSE_DATE,
                yes = Navigation(FullyVaccinated),
                no = FollowupQuestion(
                    QuestionNode(
                        CLINICAL_TRIAL,
                        yes = Navigation(FullyVaccinated),
                        no = Navigation(Isolating)
                    )
                )
            )
        ),
        no = FollowupQuestion(
            QuestionNode(
                CLINICAL_TRIAL,
                yes = Navigation(FullyVaccinated),
                no = Navigation(Isolating)
            )
        )
    )

    private var answers = listOf<Answer>()

    init {
        viewModelScope.launch {
            val questionnaire = if (isWales()) welshQuestionnaire else englishQuestionnaire
            answers = listOf(Answer(questionnaire, answer = null))
            val lastDoseDateLimit = getLastDoseDateLimit()
            if (lastDoseDateLimit != null) {
                val isActiveContactCaseOnly = !isolationStateMachine.readLogicalState().isActiveIndexCase(clock)
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
        updateViewState(FULLY_VACCINATED, option)
    }

    fun onLastDoseDateOptionChanged(option: BinaryRadioGroupOption) {
        updateViewState(DOSE_DATE, option)
    }

    fun onClinicalTrialOptionChanged(option: BinaryRadioGroupOption) {
        updateViewState(CLINICAL_TRIAL, option)
    }

    fun onMedicallyExemptOptionChanged(option: BinaryRadioGroupOption) {
        updateViewState(MEDICALLY_EXEMPT, option)
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
            if (updatedAnswer.answer == YES && updatedAnswer.questionNode.yes is FollowupQuestion) {
                add(Answer(updatedAnswer.questionNode.yes.questionNode, answer = null))
            } else if (updatedAnswer.answer == NO && updatedAnswer.questionNode.no is FollowupQuestion) {
                add(Answer(updatedAnswer.questionNode.no.questionNode, answer = null))
            }
        }
    }

    fun onClickContinue() {
        viewModelScope.launch {
            val lastAnswer = answers.last()
            val navigationTarget =
                if (lastAnswer.answer == YES && lastAnswer.questionNode.yes is Navigation) {
                    lastAnswer.questionNode.yes.navigationTarget
                } else if (lastAnswer.answer == NO && lastAnswer.questionNode.no is Navigation) {
                    lastAnswer.questionNode.no.navigationTarget
                } else {
                    null
                }

            if (navigationTarget != null) {
                acknowledgeRiskyContact()
                if (navigationTarget is FullyVaccinated || navigationTarget is MedicallyExempt) {
                    optOutOfContactIsolation()
                }
                viewStateLiveData.postValue(viewStateLiveData.value?.copy(showError = false))
                navigateToNextScreen(navigationTarget)
            } else {
                viewStateLiveData.postValue(viewStateLiveData.value?.copy(showError = true))
            }
        }
    }

    private suspend fun getLocalAuthority(): PostCodeDistrict? {
        return localAuthorityPostCodeProvider.getPostCodeDistrict()
    }

    private suspend fun isWales(): Boolean {
        return getLocalAuthority() == WALES
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

    enum class QuestionType {
        FULLY_VACCINATED, DOSE_DATE, CLINICAL_TRIAL, MEDICALLY_EXEMPT
    }

    data class Question(val questionType: QuestionType, val state: BinaryRadioGroupOption?)

    sealed class SelectionOutcome {
        data class FollowupQuestion(val questionNode: QuestionNode) : SelectionOutcome()
        data class Navigation(val navigationTarget: NavigationTarget) : SelectionOutcome()
    }

    data class QuestionNode(
        val questionType: QuestionType,
        val yes: SelectionOutcome,
        val no: SelectionOutcome
    )

    data class Answer(val questionNode: QuestionNode, val answer: BinaryRadioGroupOption?)

    sealed class NavigationTarget {
        object FullyVaccinated : NavigationTarget()
        object Isolating : NavigationTarget()
        object MedicallyExempt : NavigationTarget()
        object Finish : NavigationTarget()
    }
}
