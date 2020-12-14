package uk.nhs.nhsx.covid19.android.app.exposure

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeKeys
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class ShareKeysInformationViewModel @Inject constructor(
    private val fetchTemporaryExposureKeys: FetchTemporaryExposureKeys,
    private val submitFakeKeys: SubmitFakeKeys,
    private val stateMachine: IsolationStateMachine
) : ViewModel() {

    lateinit var testResult: ReceivedTestResult
    var exposureNotificationWasInitiallyDisabled = false
    var handleSubmitKeyResolutionStarted = false

    private val fetchKeysLiveData = SingleLiveEvent<TemporaryExposureKeysFetchResult>()
    fun fetchKeysResult(): LiveData<TemporaryExposureKeysFetchResult> = fetchKeysLiveData

    fun fetchKeys() {
        viewModelScope.launch {
            val exposureKeysFetchResult = fetchTemporaryExposureKeys()
            fetchKeysLiveData.postValue(exposureKeysFetchResult)
        }
    }

    fun onKeysNotSubmitted() {
        submitFakeKeys()
    }

    fun acknowledgeTestResult() {
        stateMachine.processEvent(
            OnTestResultAcknowledge(testResult, removeTestResult = false)
        )
    }
}
