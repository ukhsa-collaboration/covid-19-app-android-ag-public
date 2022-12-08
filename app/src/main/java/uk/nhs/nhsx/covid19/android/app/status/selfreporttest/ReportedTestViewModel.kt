package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.SymptomsOnset
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_2 as NO

class ReportedTestViewModel @AssistedInject constructor(
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private var reportedTestOption: BinaryVerticalRadioGroupOption? = null
    private var hasError: Boolean = false

    init {
        questions.hasReportedResult?.let { hasReportedResult ->
            reportedTestOption = when (hasReportedResult) {
                true -> YES
                else -> NO
            }
            updateViewState()
        }
    }

    fun onReportedTestOptionChecked(option: BinaryVerticalRadioGroupOption?) {
        reportedTestOption = option
        updateViewState()
    }

    private fun updateViewState() {
        viewStateLiveData.postValue(ViewState(reportedTestOption, hasError))
    }

    fun onBackPressed() {
        when {
            questions.symptomsOnsetDate != null -> {
                navigateLiveData.postValue(SymptomsOnset(questions))
            }
            questions.hadSymptoms != null -> {
                navigateLiveData.postValue(Symptoms(questions))
            }
            else -> {
                navigateLiveData.postValue(TestDate(questions))
            }
        }
    }

    fun onClickContinue() {
        when (reportedTestOption) {
            YES -> {
                hasError = false
                navigateLiveData.postValue(
                    CheckAnswers(questions.copy(hasReportedResult = true))
                )
            }
            NO -> {
                hasError = false
                navigateLiveData.postValue(
                    CheckAnswers(questions.copy(hasReportedResult = false))
                )
            }
            null -> {
                hasError = true
                updateViewState()
            }
        }
    }

    data class ViewState(val reportedTestSelection: BinaryVerticalRadioGroupOption?, val hasError: Boolean)

    sealed class NavigationTarget {
        data class TestDate(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class Symptoms(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class SymptomsOnset(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class CheckAnswers(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): ReportedTestViewModel
    }
}
