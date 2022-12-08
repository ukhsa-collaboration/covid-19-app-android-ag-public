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
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SelfReportSymptomsOnsetViewModel @AssistedInject constructor(
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

    private var testEndDate: LocalDate =
        questions.testEndDate?.date ?: throw IllegalStateException("Cannot start without test end date")
    private var symptomsOnsetDate: SelectedDate = NotStated

    init {
        val lastPossibleSymptomsOnsetDate = testEndDate
        val firstPossibleSymptomsOnsetDate = lastPossibleSymptomsOnsetDate.minusDays(
            isolationStateMachine.readLogicalState().isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong() - 1
        )
        questions.symptomsOnsetDate?.let { symptomsOnsetDate ->
            this.symptomsOnsetDate = when (symptomsOnsetDate.rememberedDate) {
                true -> ExplicitDate(symptomsOnsetDate.date)
                false -> CannotRememberDate
            }
        }

        if (viewStateLiveData.value == null) {
            viewStateLiveData.postValue(
                ViewState(
                    selectedOnsetDate = symptomsOnsetDate,
                    hasError = false,
                    symptomsOnsetWindowDays = firstPossibleSymptomsOnsetDate..lastPossibleSymptomsOnsetDate
                )
            )
        }
    }

    fun onDateSelected(dateInMillis: Long) {
        val instant: Instant = Instant.ofEpochMilli(dateInMillis)
        val localDate = instant.toLocalDate(ZoneOffset.UTC)
        val currentState = viewStateLiveData.value ?: return
        val newState =
            currentState.copy(selectedOnsetDate = ExplicitDate(localDate), hasError = false)
        viewStateLiveData.postValue(newState)
    }

    fun isSymptomsOnsetDateValid(
        dateInMillis: Long,
        symptomsOnsetWindowDays: ClosedRange<LocalDate>
    ): Boolean {
        val date = Instant.ofEpochMilli(dateInMillis).toLocalDate(ZoneOffset.UTC)
        return date in symptomsOnsetWindowDays
    }

    fun onBackPressed() {
        navigateLiveData.postValue(Symptoms(questions))
    }

    fun onDatePickerContainerClicked() {
        datePickerContainerClickedLiveData.postValue(testEndDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    }

    fun cannotRememberDateChecked() {
        val currentState = viewStateLiveData.value ?: return
        val newState = currentState.copy(selectedOnsetDate = CannotRememberDate, hasError = false)
        viewStateLiveData.postValue(newState)
    }

    fun cannotRememberDateUnchecked() {
        val currentState = viewStateLiveData.value ?: return
        val newState = currentState.copy(selectedOnsetDate = NotStated, hasError = false)
        viewStateLiveData.postValue(newState)
    }

    fun onButtonContinueClicked() {
        val currentState = viewStateLiveData.value ?: return
        val shouldShowReportedTest = questions.isNHSTest == true && questions.testKitType == RAPID_SELF_REPORTED
        when (val symptomsOnsetDate = currentState.selectedOnsetDate) {
            is ExplicitDate -> {
                if (shouldShowReportedTest) {
                    navigateLiveData.postValue(ReportedTest(questions.copy(
                        symptomsOnsetDate = ChosenDate(true, symptomsOnsetDate.date)))
                    )
                } else {
                    navigateLiveData.postValue(CheckAnswers(questions.copy(
                        symptomsOnsetDate = ChosenDate(true, symptomsOnsetDate.date)))
                    )
                }
            }
            is CannotRememberDate -> {
                if (shouldShowReportedTest) {
                    navigateLiveData.postValue(ReportedTest(questions.copy(
                        symptomsOnsetDate = ChosenDate(false, testEndDate)))
                    )
                } else {
                    navigateLiveData.postValue(CheckAnswers(questions.copy(
                        symptomsOnsetDate = ChosenDate(false, testEndDate)))
                    )
                }
            }
            is NotStated -> {
                val newState = currentState.copy(hasError = true)
                viewStateLiveData.postValue(newState)
            }
        }
    }

    data class ViewState(
        val selectedOnsetDate: SelectedDate,
        val symptomsOnsetWindowDays: ClosedRange<LocalDate>,
        val hasError: Boolean
    )

    sealed class NavigationTarget {
        data class Symptoms(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class ReportedTest(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class CheckAnswers(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): SelfReportSymptomsOnsetViewModel
    }
}
