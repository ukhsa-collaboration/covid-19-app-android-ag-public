package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_1 as POSITIVE
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_2 as NEGATIVE
import uk.nhs.nhsx.covid19.android.app.widgets.TripleVerticalRadioGroup.TripleVerticalRadioGroupOption.OPTION_3 as VOID
import javax.inject.Inject

class TestTypeViewModel @Inject constructor() : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private var selfReportTestQuestions: SelfReportTestQuestions = SelfReportTestQuestions(null,
        null, null, null, null, null, null, null)

    private var testTypeOption: TripleVerticalRadioGroupOption? = null
    private var hasError: Boolean = false

    fun onCreate(questions: SelfReportTestQuestions? = null) {
        if (questions != null) {
            selfReportTestQuestions = questions
            if (questions.testType != null) {
                testTypeOption = when (questions.testType) {
                    VirologyTestResult.POSITIVE -> POSITIVE
                    VirologyTestResult.NEGATIVE -> NEGATIVE
                    else -> VOID
                }
                updateViewState()
            }
        }
    }

    fun onTestTypeOptionChecked(option: TripleVerticalRadioGroupOption?) {
        testTypeOption = option
        updateViewState()
    }

    private fun updateViewState() {
        viewModelScope.launch {
            viewStateLiveData.postValue(ViewState(testTypeOption, hasError))
        }
    }

    fun onClickContinue() {
        when (testTypeOption) {
            POSITIVE -> {
                hasError = false
                navigateLiveData.postValue(
                    NavigationTarget.PositiveTest(selfReportTestQuestions.copy(testType = VirologyTestResult.POSITIVE)))
            }
            NEGATIVE -> {
                hasError = false
                navigateLiveData.postValue(
                    NavigationTarget.NegativeTest(isNegative = true))
            }
            VOID -> {
                hasError = false
                navigateLiveData.postValue(
                    NavigationTarget.VoidTest(isNegative = false))
            }
            null -> {
                hasError = true
                updateViewState()
            }
        }
    }

    data class ViewState(val testTypeSelection: TripleVerticalRadioGroupOption?, val hasError: Boolean)

    sealed class NavigationTarget {
        data class PositiveTest(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class NegativeTest(val isNegative: Boolean) : NavigationTarget()
        data class VoidTest(val isNegative: Boolean) : NavigationTarget()
    }
}
