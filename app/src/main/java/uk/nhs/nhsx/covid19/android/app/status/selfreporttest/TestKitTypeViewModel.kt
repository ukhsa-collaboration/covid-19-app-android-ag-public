package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.DeclinedKeySharing
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.ShareKeysInfo
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.TestOrigin
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_1 as LFD
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_2 as PCR
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption

class TestKitTypeViewModel @AssistedInject constructor(
    @Assisted private val questions: SelfReportTestQuestions
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private var navigateLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigate(): LiveData<NavigationTarget> = navigateLiveData

    private var testKitTypeOption: BinaryVerticalRadioGroupOption? = null
    private var hasError: Boolean = false

    init {
        questions.testKitType?.let { testKitType ->
            testKitTypeOption = when (testKitType) {
                LAB_RESULT -> PCR
                else -> LFD
            }
            updateViewState()
        }
    }

    fun onTestKitTypeOptionChecked(option: BinaryVerticalRadioGroupOption?) {
        testKitTypeOption = option
        updateViewState()
    }

    private fun updateViewState() {
        viewStateLiveData.postValue(ViewState(testKitTypeOption, hasError))
    }

    fun onClickContinue() {
        when (testKitTypeOption) {
            LFD -> {
                hasError = false
                navigateLiveData.postValue(
                    TestOrigin(questions.copy(testKitType = RAPID_SELF_REPORTED))
                )
            }
            PCR -> {
                hasError = false
                navigateLiveData.postValue(
                    TestDate(questions.copy(testKitType = LAB_RESULT, isNHSTest = null, hasReportedResult = null))
                )
            }
            null -> {
                hasError = true
                updateViewState()
            }
        }
    }

    fun onBackPressed() {
        if (questions.temporaryExposureKeys != null) {
            navigateLiveData.postValue(ShareKeysInfo(questions))
        } else {
            navigateLiveData.postValue(DeclinedKeySharing(questions))
        }
    }

    data class ViewState(val testKitTypeSelection: BinaryVerticalRadioGroupOption?, val hasError: Boolean)

    sealed class NavigationTarget {
        data class TestOrigin(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class TestDate(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class DeclinedKeySharing(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
        data class ShareKeysInfo(val selfReportTestQuestions: SelfReportTestQuestions) : NavigationTarget()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            questions: SelfReportTestQuestions,
        ): TestKitTypeViewModel
    }
}
