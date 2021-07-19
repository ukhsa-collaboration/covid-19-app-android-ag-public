package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAskForSymptomsOnPositiveTestEntry
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidHaveSymptomsBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class LinkTestResultSymptomsViewModel @Inject constructor(
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel() {

    private val confirmSymptomsLiveData = SingleLiveEvent<Unit>()
    fun confirmSymptoms(): LiveData<Unit> = confirmSymptomsLiveData

    private var onCreateWasCalled = false

    fun onCreate() {
        if (!onCreateWasCalled) {
            analyticsEventProcessor.track(DidAskForSymptomsOnPositiveTestEntry)
            onCreateWasCalled = true
        }
    }

    fun onConfirmSymptomsClicked() {
        analyticsEventProcessor.track(DidHaveSymptomsBeforeReceivedTestResult)
        confirmSymptomsLiveData.postCall()
    }
}
