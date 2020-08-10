package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.NegativeHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.PositiveHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnPositiveSelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class ReviewSymptomsViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val riskCalculator: RiskCalculator,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel() {

    @VisibleForTesting
    internal val viewState = MutableLiveData<ViewState>(
        ViewState(
            reviewSymptomItems = listOf(),
            onsetDate = NotStated,
            showOnsetDateError = false,
            symptomsOnsetWindowDays = 0
        )
    )

    fun viewState(): LiveData<ViewState> = viewState

    private val navigateToIsolationScreen = SingleLiveEvent<Boolean>()
    fun navigateToIsolationScreen(): LiveData<Boolean> = navigateToIsolationScreen

    @VisibleForTesting
    internal var riskThreshold = 0.0F

    fun setup(
        questions: List<Question>,
        riskThreshold: Float,
        symptomsOnsetWindowDays: Int
    ) {
        val reviewSymptomItems = generateReviewSymptomItems(questions)
        this.riskThreshold = riskThreshold
        val currentState = viewState.value ?: return
        val newState = currentState.copy(
            reviewSymptomItems = reviewSymptomItems,
            symptomsOnsetWindowDays = symptomsOnsetWindowDays
        )
        viewState.postValue(newState)
    }

    private fun generateReviewSymptomItems(questions: List<Question>): List<ReviewSymptomItem> {
        val (checked, unchecked) = questions.partition { it.isChecked }
        return mutableListOf<ReviewSymptomItem>().apply {
            add(PositiveHeader)
            addAll(checked)
            if (unchecked.isNotEmpty()) {
                add(NegativeHeader)
                addAll(unchecked)
            }
        }
    }

    fun onDateSelected(dateInMillis: Long) {
        val instant: Instant = Instant.ofEpochMilli(dateInMillis)
        val localDate = instant.atZone(ZoneOffset.UTC).toLocalDate()
        val currentState = viewState.value ?: return
        val newState =
            currentState.copy(onsetDate = ExplicitDate(localDate), showOnsetDateError = false)
        viewState.postValue(newState)
    }

    fun cannotRememberDateChecked() {
        val currentState = viewState.value ?: return
        val newState = currentState.copy(onsetDate = CannotRememberDate, showOnsetDateError = false)
        viewState.postValue(newState)
    }

    fun cannotRememberDateUnchecked() {
        val currentState = viewState.value ?: return
        val onsetDate = if (currentState.onsetDate is ExplicitDate) {
            currentState.onsetDate
        } else {
            NotStated
        }
        val newState = currentState.copy(onsetDate = onsetDate, showOnsetDateError = false)
        viewState.postValue(newState)
    }

    fun onButtonConfirmedClicked() {
        viewModelScope.launch {
            val currentState = viewState.value ?: return@launch
            if (currentState.onsetDate == NotStated) {
                val newState = currentState.copy(showOnsetDateError = true)
                viewState.postValue(newState)
            } else {
                val userHaveCoronavirusSymptoms = doesUserHaveCoronavirusSymptoms()
                if (userHaveCoronavirusSymptoms) {
                    transitionToIndexedCase()
                    analyticsEventProcessor.track(CompletedQuestionnaireAndStartedIsolation)
                } else {
                    analyticsEventProcessor.track(CompletedQuestionnaireButDidNotStartIsolation)
                }
                val isolationState = isolationStateMachine.readState()
                navigateToIsolationScreen.postValue(isolationState !is Default)
            }
        }
    }

    private fun doesUserHaveCoronavirusSymptoms(): Boolean {
        val currentState = viewState.value ?: return false
        val selectedSymptoms = currentState.reviewSymptomItems
            .filterIsInstance<Question>()
            .filter { it.isChecked }
            .map { it.symptom }

        return riskCalculator.isRiskAboveThreshold(selectedSymptoms, riskThreshold)
    }

    private fun transitionToIndexedCase() {
        val currentState = viewState.value ?: return
        val symptomsOnsetDate = currentState.onsetDate

        isolationStateMachine.processEvent(
            OnPositiveSelfAssessment(symptomsOnsetDate)
        )
    }

    data class ViewState(
        val reviewSymptomItems: List<ReviewSymptomItem>,
        val onsetDate: SelectedDate,
        val showOnsetDateError: Boolean,
        val symptomsOnsetWindowDays: Int
    )
}

sealed class SelectedDate {
    object NotStated : SelectedDate()
    data class ExplicitDate(val date: LocalDate) : SelectedDate()
    object CannotRememberDate : SelectedDate()
}
