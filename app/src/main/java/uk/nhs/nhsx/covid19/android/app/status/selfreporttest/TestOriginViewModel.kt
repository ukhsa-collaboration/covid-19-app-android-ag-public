package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestOriginViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestOriginViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_2 as NO

class TestOriginViewModel @AssistedInject constructor(
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private var testOriginOption: BinaryVerticalRadioGroupOption? = null
    private var hasError: Boolean = false

    init {
        questions.isNHSTest?.let { isNHSTest ->
            testOriginOption = when (isNHSTest) {
                true -> YES
                false -> NO
            }
            updateViewState()
        }
    }

    fun onTestOriginOptionChecked(option: BinaryVerticalRadioGroupOption?) {
        testOriginOption = option
        updateViewState()
    }

    fun onClickContinue() {
        when (testOriginOption) {
            YES -> {
                hasError = false
                navigateLiveData.postValue(
                    TestDate(questions.copy(isNHSTest = true))
                )
            }
            NO -> {
                hasError = false
                navigateLiveData.postValue(
                    TestDate(questions.copy(isNHSTest = false, hasReportedResult = null))
                )
            }
            null -> {
                hasError = true
                updateViewState()
            }
        }
    }

    fun onBackPressed() {
        navigateLiveData.postValue(TestKitType(questions))
    }

    private fun updateViewState() {
        viewStateLiveData.postValue(ViewState(testOriginOption, hasError))
    }

    data class ViewState(val testOriginSelection: BinaryVerticalRadioGroupOption?, val hasError: Boolean)

    sealed class NavigationTarget {
        data class TestDate(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class TestKitType(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): TestOriginViewModel
    }
}
