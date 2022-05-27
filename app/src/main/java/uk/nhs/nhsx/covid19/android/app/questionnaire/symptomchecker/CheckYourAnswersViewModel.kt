package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedV2SymptomsQuestionnaire
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.HowYouFeel
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.Next
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.YourSymptoms
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.LocalDate

class CheckYourAnswersViewModel @AssistedInject constructor(
    @Assisted private val questions: SymptomsCheckerQuestions,
    private val symptomCheckerAdviceHandler: SymptomCheckerAdviceHandler,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val lastCompletedV2SymptomsQuestionnaireDateProvider: LastCompletedV2SymptomsQuestionnaireDateProvider,
    private val clock: Clock
) : ViewModel() {

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    init {
        updateViewState()
    }

    private fun updateViewState() {
        viewModelScope.launch {
            viewStateLiveData.postValue(ViewState(questions))
        }
    }

    fun onClickSubmitAnswers() {
        val symptomsCheckerAdviceResult = symptomCheckerAdviceHandler.invoke(questions)

        symptomsCheckerAdviceResult?.let { result ->
            navigateLiveData.postValue(
                Next(symptomsCheckerQuestions = questions, result)
            )
        } ?: throw IllegalStateException("Symptom checker advice result not present on submit")
        analyticsEventProcessor.track(CompletedV2SymptomsQuestionnaire)
        lastCompletedV2SymptomsQuestionnaireDateProvider.lastCompletedV2SymptomsQuestionnaire = LastCompletedV2SymptomsQuestionnaireDate(
            LocalDate.now(clock)
        )
    }

    fun onClickYourSymptomsChange() {
        navigateLiveData.postValue(
            YourSymptoms(symptomsCheckerQuestions = questions)
        )
    }

    fun onClickHowYouFeelChange() {
        navigateLiveData.postValue(
            HowYouFeel(symptomsCheckerQuestions = questions)
        )
    }

    data class ViewState(val symptomsCheckerQuestions: SymptomsCheckerQuestions)

    sealed class NavigationTarget {
        data class Next(val symptomsCheckerQuestions: SymptomsCheckerQuestions, val result: SymptomCheckerAdviceResult) : NavigationTarget()
        data class YourSymptoms(val symptomsCheckerQuestions: SymptomsCheckerQuestions) : NavigationTarget()
        data class HowYouFeel(val symptomsCheckerQuestions: SymptomsCheckerQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SymptomsCheckerQuestions,
        ): CheckYourAnswersViewModel
    }
}
