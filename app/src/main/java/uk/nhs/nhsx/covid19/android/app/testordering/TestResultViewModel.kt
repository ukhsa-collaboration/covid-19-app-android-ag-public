package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.SubmitResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.remainingDaysInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_IGNORE
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_NEGATIVE_IN_ISOLATION
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_NEGATIVE_NOT_IN_ISOLATION
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_POSITIVE_IN_ISOLATION
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.RESULT_POSITIVE_NOT_IN_ISOLATION
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class TestResultViewModel @Inject constructor(
    private val latestTestResultProvider: LatestTestResultProvider,
    private val stateMachine: IsolationStateMachine,
    private val submitTemporaryExposureKeys: SubmitTemporaryExposureKeys,
    private val keyWindowCalculator: KeyWindowCalculator
) : ViewModel() {

    private val viewState = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewState

    private val keyUploadResult = SingleLiveEvent<SubmitResult>()
    fun keyUploadResult(): LiveData<SubmitResult> = keyUploadResult

    fun onCreate() {
        val virologyTestResult = latestTestResultProvider.latestTestResult?.testResult
        val inIsolationNow = stateMachine.readState() is Isolation
        val mainState = if (virologyTestResult == POSITIVE && inIsolationNow) {
            RESULT_POSITIVE_IN_ISOLATION
        } else if (virologyTestResult == POSITIVE && !inIsolationNow) {
            RESULT_POSITIVE_NOT_IN_ISOLATION
        } else if (virologyTestResult == NEGATIVE && inIsolationNow) {
            RESULT_NEGATIVE_IN_ISOLATION
        } else if (virologyTestResult == NEGATIVE && !inIsolationNow) {
            RESULT_NEGATIVE_NOT_IN_ISOLATION
        } else {
            RESULT_IGNORE
        }
        val remainingDaysInIsolation = stateMachine.remainingDaysInIsolation().toInt()
        viewState.postValue(ViewState(mainState, remainingDaysInIsolation))
    }

    fun submitKeys() {
        viewModelScope.launch {
            val dateWindow = keyWindowCalculator.calculateDateWindow()
            if (dateWindow == null) {
                keyUploadResult.postValue(SubmitResult.Failure(IllegalStateException("Can't calculate date window")))
                return@launch
            }

            val result = submitTemporaryExposureKeys.invoke(dateWindow)
            keyUploadResult.postValue(result)
        }
    }

    data class ViewState(
        val mainState: MainState,
        val remainingDaysInIsolation: Int
    )

    enum class MainState {
        RESULT_NEGATIVE_IN_ISOLATION,
        RESULT_NEGATIVE_NOT_IN_ISOLATION,
        RESULT_POSITIVE_IN_ISOLATION,
        RESULT_POSITIVE_NOT_IN_ISOLATION,
        RESULT_IGNORE
    }
}
