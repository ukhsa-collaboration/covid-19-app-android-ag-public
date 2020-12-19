package uk.nhs.nhsx.covid19.android.app.exposure

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.STORE_EXPOSURE_WINDOWS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW_AFTER_POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW_POSITIVE_TEST
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.indexCaseOnsetDateBeforeTestResultDate
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeKeys
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

class ShareKeysInformationViewModel @Inject constructor(
    private val fetchTemporaryExposureKeys: FetchTemporaryExposureKeys,
    private val submitFakeKeys: SubmitFakeKeys,
    private val stateMachine: IsolationStateMachine,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val submitEpidemiologyData: SubmitEpidemiologyData,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows,
    private val clock: Clock
) : ViewModel() {

    lateinit var testResult: ReceivedTestResult
    var exposureNotificationWasInitiallyDisabled = false
    var handleSubmitKeyResolutionStarted = false

    private val fetchKeysLiveData = SingleLiveEvent<TemporaryExposureKeysFetchResult>()
    fun fetchKeysResult(): LiveData<TemporaryExposureKeysFetchResult> = fetchKeysLiveData

    fun fetchKeys() {
        viewModelScope.launch {
            val testResultDate = LocalDateTime.ofInstant(testResult.testEndDate, clock.zone).toLocalDate()
            val onsetDateBasedOnTestEndDate = testResultDate.minusDays(indexCaseOnsetDateBeforeTestResultDate)
            val exposureKeysFetchResult = fetchTemporaryExposureKeys(onsetDateBasedOnTestEndDate)
            Log.d("ShareKeys", exposureKeysFetchResult.toString())
            fetchKeysLiveData.postValue(exposureKeysFetchResult)
        }
    }

    fun onSubmitKeysDenied() {
        submitFakeKeys()
        submitFakeExposureWindows(EXPOSURE_WINDOW_AFTER_POSITIVE, 0)
        acknowledgeTestResult()
    }

    fun onSubmitKeysSuccess() {
        acknowledgeTestResult()
        if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS)) {
            submitEpidemiologyData(
                epidemiologyEventProvider.epidemiologyEvents,
                epidemiologyEventType = EXPOSURE_WINDOW_POSITIVE_TEST
            )
        }
    }

    private fun acknowledgeTestResult() {
        stateMachine.processEvent(OnTestResultAcknowledge(testResult, removeTestResult = false))
    }
}
