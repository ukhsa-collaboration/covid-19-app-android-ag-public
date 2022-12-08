package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsOnsetViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.SymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class SelfReportSymptomsViewModel @AssistedInject constructor(
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private var symptomsOption: BinaryRadioGroupOption? = null
    private var hasError: Boolean = false

    init {
        questions.hadSymptoms?.let { hadSymptoms ->
            symptomsOption = when (hadSymptoms) {
                true -> YES
                false -> NO
            }
            updateViewState()
        }
    }

    fun onSymptomsOptionChecked(option: BinaryRadioGroupOption?) {
        symptomsOption = option
        updateViewState()
    }

    private fun updateViewState() {
        viewStateLiveData.postValue(ViewState(symptomsOption, hasError))
    }

    fun onClickContinue() {
        when (symptomsOption) {
            YES -> {
                hasError = false
                navigateLiveData.postValue(SymptomOnsetDate(questions.copy(hadSymptoms = true)))
            }
            NO -> {
                hasError = false
                if (questions.isNHSTest == true && questions.testKitType == RAPID_SELF_REPORTED) {
                    navigateLiveData.postValue(ReportedTest(questions.copy(hadSymptoms = false, symptomsOnsetDate = null)))
                } else {
                    navigateLiveData.postValue(CheckAnswers(questions.copy(hadSymptoms = false, symptomsOnsetDate = null)))
                }
            }
            null -> {
                hasError = true
                updateViewState()
            }
        }
    }

    fun onBackPressed() {
        navigateLiveData.postValue(TestDate(questions))
    }

    data class ViewState(val symptomsSelection: BinaryRadioGroupOption?, val hasError: Boolean)

    sealed class NavigationTarget {
        data class TestDate(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class SymptomOnsetDate(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class ReportedTest(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class CheckAnswers(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): SelfReportSymptomsViewModel
    }
}
