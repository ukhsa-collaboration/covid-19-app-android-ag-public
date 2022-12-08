package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.TestOrigin
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SelectTestDateViewModel @AssistedInject constructor(
    private val clock: Clock,
    private val isolationStateMachine: IsolationStateMachine,
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private val datePickerContainerClickedLiveData = SingleLiveEvent<Long>()
    fun datePickerContainerClicked() = datePickerContainerClickedLiveData

    private var testEndDate: SelectedDate = NotStated
    private var lastPossibleTestDate: LocalDate = LocalDate.now(clock)

    init {
        val firstPossibleTestDate = lastPossibleTestDate.minusDays(
            isolationStateMachine.readLogicalState().isolationConfiguration.indexCaseSinceTestResultEndDate.toLong() - 1
        )
        questions.testEndDate?.let { testEndDate ->
            this.testEndDate = when (testEndDate.rememberedDate) {
                true -> ExplicitDate(testEndDate.date)
                false -> CannotRememberDate
            }
        }

        if (viewStateLiveData.value == null) {
            viewStateLiveData.postValue(
                ViewState(
                    selectedTestDate = testEndDate,
                    hasError = false,
                    testDateWindowDays = firstPossibleTestDate..lastPossibleTestDate
                )
            )
        }
    }

    fun onBackPressed() {
        if (questions.testKitType == RAPID_SELF_REPORTED) {
            navigateLiveData.postValue(TestOrigin(questions))
        } else {
            navigateLiveData.postValue(TestKitType(questions))
        }
    }

    fun onDateSelected(dateInMillis: Long) {
        val instant: Instant = Instant.ofEpochMilli(dateInMillis)
        val localDate = instant.toLocalDate(ZoneOffset.UTC)
        val currentState = viewStateLiveData.value ?: return
        val newState =
            currentState.copy(selectedTestDate = ExplicitDate(localDate), hasError = false)
        viewStateLiveData.postValue(newState)
    }

    fun isTestDateValid(
        dateInMillis: Long,
        testDateWindowDays: ClosedRange<LocalDate>
    ): Boolean {
        val date = Instant.ofEpochMilli(dateInMillis).toLocalDate(ZoneOffset.UTC)
        return date in testDateWindowDays
    }

    fun onDatePickerContainerClicked() {
        datePickerContainerClickedLiveData.postValue(lastPossibleTestDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    }

    fun cannotRememberDateChecked() {
        val currentState = viewStateLiveData.value ?: return
        val newState = currentState.copy(selectedTestDate = CannotRememberDate, hasError = false)
        viewStateLiveData.postValue(newState)
    }

    fun cannotRememberDateUnchecked() {
        val currentState = viewStateLiveData.value ?: return
        val newState = currentState.copy(selectedTestDate = NotStated, hasError = false)
        viewStateLiveData.postValue(newState)
    }

    fun onButtonContinueClicked() {
        val currentState = viewStateLiveData.value ?: return
        val isolationState = isolationStateMachine.readLogicalState()
        val isInIsolation = isolationState is PossiblyIsolating && isolationState.isActiveIsolation(clock)
        when (val testEndDate = currentState.selectedTestDate) {
            is ExplicitDate -> {
                val explicitChosenDate = ChosenDate(true, testEndDate.date)
                val explicitNavTarget = when {
                    isInIsolation -> {
                        setIsInIsolationNavigation(explicitChosenDate)
                    }
                    hasChangedDateFromPreviouslySaved(testEndDate.date) -> {
                        Symptoms(questions.copy(testEndDate = explicitChosenDate, symptomsOnsetDate = null))
                    }
                    else -> {
                        Symptoms(questions.copy(testEndDate = explicitChosenDate))
                    }
                }
                navigateLiveData.postValue(explicitNavTarget)
            }
            is CannotRememberDate -> {
                val cannotRememberChosenDate = ChosenDate(false, lastPossibleTestDate)
                val cannotRememberNavTarget = when {
                    isInIsolation -> {
                        setIsInIsolationNavigation(cannotRememberChosenDate)
                    }
                    hasChangedDateFromPreviouslySaved(lastPossibleTestDate) -> {
                        Symptoms(questions.copy(testEndDate = cannotRememberChosenDate, symptomsOnsetDate = null))
                    }
                    else -> {
                        Symptoms(questions.copy(testEndDate = cannotRememberChosenDate))
                    }
                }
                navigateLiveData.postValue(cannotRememberNavTarget)
            }
            is NotStated -> {
                val newState = currentState.copy(hasError = true)
                viewStateLiveData.postValue(newState)
            }
        }
    }

    private fun setChosenDateAndNullSymptomsAnswers(chosenDate: ChosenDate): SelfReportTestQuestions {
        return questions.copy(testEndDate = chosenDate, hadSymptoms = null, symptomsOnsetDate = null)
    }

    private fun setIsInIsolationNavigation(chosenDate: ChosenDate): NavigationTarget {
        return if (questions.testKitType == RAPID_SELF_REPORTED && questions.isNHSTest == true) {
            ReportedTest(setChosenDateAndNullSymptomsAnswers(chosenDate))
        } else {
            CheckAnswers(setChosenDateAndNullSymptomsAnswers(chosenDate))
        }
    }

    private fun hasChangedDateFromPreviouslySaved(newDate: LocalDate): Boolean {
        return if (questions.testEndDate?.date != null) {
            !questions.testEndDate.date.isEqual(newDate)
        } else {
            false
        }
    }

    data class ViewState(val selectedTestDate: SelectedDate, val testDateWindowDays: ClosedRange<LocalDate>, val hasError: Boolean)

    sealed class NavigationTarget {
        data class CheckAnswers(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class ReportedTest(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class Symptoms(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class TestOrigin(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class TestKitType(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): SelectTestDateViewModel
    }
}
