package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.NegativeHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.PositiveHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ReviewSymptomsViewModel @AssistedInject constructor(
    private val questionnaireIsolationHandler: QuestionnaireIsolationHandler,
    private val clock: Clock,
    @Assisted private val questions: List<Question>,
    @Assisted private val riskThreshold: Float,
    @Assisted private val symptomsOnsetWindowDays: Int
) : ViewModel() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private val navigateToSymptomAdviceScreen = SingleLiveEvent<SymptomAdvice>()
    fun navigateToSymptomAdviceScreen(): LiveData<SymptomAdvice> = navigateToSymptomAdviceScreen

    init {
        viewState.postValue(
            ViewState(
                reviewSymptomItems = generateReviewSymptomItems(questions),
                onsetDate = NotStated,
                showOnsetDateError = false,
                symptomsOnsetWindowDays = symptomsOnsetWindowDays,
                showOnsetDatePicker = false,
                datePickerSelection = clock.millis()
            )
        )
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
        val localDate = instant.toLocalDate(ZoneOffset.UTC)
        val currentState = viewState.value ?: return
        val newState =
            currentState.copy(onsetDate = ExplicitDate(localDate), showOnsetDateError = false, showOnsetDatePicker = false)
        viewState.postValue(newState)
    }

    fun cannotRememberDateChecked() {
        val currentState = viewState.value ?: return
        val newState = currentState.copy(onsetDate = CannotRememberDate, showOnsetDateError = false, showOnsetDatePicker = false)
        viewState.postValue(newState)
    }

    fun cannotRememberDateUnchecked() {
        val currentState = viewState.value ?: return
        val onsetDate = if (currentState.onsetDate is ExplicitDate) {
            currentState.onsetDate
        } else {
            NotStated
        }
        val newState = currentState.copy(onsetDate = onsetDate, showOnsetDateError = false, showOnsetDatePicker = false)
        viewState.postValue(newState)
    }

    fun onButtonConfirmedClicked() {
        viewModelScope.launch {
            val currentState = viewState.value ?: return@launch
            if (currentState.onsetDate == NotStated) {
                val newState = currentState.copy(showOnsetDateError = true)
                viewState.postValue(newState)
            } else {
                val symptomAdvice = questionnaireIsolationHandler.computeAdvice(
                    riskThreshold = riskThreshold,
                    selectedSymptoms = getSelectedSymptoms(),
                    onsetDate = currentState.onsetDate
                )
                navigateToSymptomAdviceScreen.postValue(symptomAdvice)
            }
        }
    }

    private fun getSelectedSymptoms(): List<Symptom> =
        viewState.value?.reviewSymptomItems?.toSelectedSymptoms() ?: listOf()

    fun isOnsetDateValid(date: Long, symptomsOnsetWindowDays: Int): Boolean =
        date <= Instant.now(clock).toEpochMilli() &&
            date > Instant.now(clock).minus(symptomsOnsetWindowDays.toLong(), ChronoUnit.DAYS).toEpochMilli()

    fun onDatePickerDismissed() {
        val currentState = viewState.value ?: return
        val newState = currentState.copy(showOnsetDatePicker = false)
        viewState.postValue(newState)
    }

    fun onSelectDateClicked() {
        val currentState = viewState.value ?: return
        val newState = currentState.copy(showOnsetDatePicker = true)
        viewState.postValue(newState)
    }

    data class ViewState(
        val reviewSymptomItems: List<ReviewSymptomItem>,
        val onsetDate: SelectedDate,
        val showOnsetDateError: Boolean,
        val symptomsOnsetWindowDays: Int,
        val showOnsetDatePicker: Boolean,
        val datePickerSelection: Long
    )

    @AssistedFactory
    interface Factory {
        fun create(
            questions: List<Question>,
            riskThreshold: Float,
            symptomsOnsetWindowDays: Int
        ): ReviewSymptomsViewModel
    }
}

fun List<ReviewSymptomItem>.toSelectedSymptoms(): List<Symptom> =
    this.filterIsInstance<Question>()
        .filter { it.isChecked }
        .map { it.symptom }

sealed class SelectedDate {
    object NotStated : SelectedDate()
    data class ExplicitDate(val date: LocalDate) : SelectedDate()
    object CannotRememberDate : SelectedDate()
}
