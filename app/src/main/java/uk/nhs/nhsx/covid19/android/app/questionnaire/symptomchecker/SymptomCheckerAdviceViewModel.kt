package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedV2SymptomsQuestionnaireAndStayAtHome
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.CONTINUE_NORMAL_ACTIVITIES
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.TRY_TO_STAY_AT_HOME
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.NavigationTarget.BackToHome
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.NavigationTarget.BackToQuestionnaire
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.ViewState.ContinueNormalActivities
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.ViewState.TryToStayAtHome
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.LocalDate

class SymptomCheckerAdviceViewModel @AssistedInject constructor(
    @Assisted val questions: SymptomsCheckerQuestions,
    @Assisted val result: SymptomCheckerAdviceResult,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val lastCompletedV2SymptomsQuestionnaireDateProvider: LastCompletedV2SymptomsQuestionnaireDateProvider,
    private val clock: Clock
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    init {
        updateViewState()
    }

    private fun updateViewState() {
        val viewState = when (result) {
            TRY_TO_STAY_AT_HOME -> TryToStayAtHome
            CONTINUE_NORMAL_ACTIVITIES -> ContinueNormalActivities
        }

        if (result == TRY_TO_STAY_AT_HOME) {
            analyticsEventProcessor.track(CompletedV2SymptomsQuestionnaireAndStayAtHome)
            lastCompletedV2SymptomsQuestionnaireDateProvider.lastCompletedV2SymptomsQuestionnaireAndStayAtHome =
                LastCompletedV2SymptomsQuestionnaireAndStayAtHomeDate(
                    LocalDate.now(clock)
                )
        }

        viewStateLiveData.postValue(viewState)
    }

    fun onBackPressed() {
        navigateLiveData.postValue(BackToQuestionnaire(questions))
    }

    fun onFinishPressed() {
        navigateLiveData.postValue(BackToHome)
    }

    sealed class ViewState {
        object TryToStayAtHome : ViewState()
        object ContinueNormalActivities : ViewState()
    }

    sealed class NavigationTarget {
        data class BackToQuestionnaire(val symptomsCheckerQuestions: SymptomsCheckerQuestions) : NavigationTarget()
        object BackToHome : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SymptomsCheckerQuestions,
            result: SymptomCheckerAdviceResult
        ): SymptomCheckerAdviceViewModel
    }
}
