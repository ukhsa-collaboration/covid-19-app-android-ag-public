package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionViewModel.ExposedNotificationResult.ConsentConfirmation
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionViewModel.ExposedNotificationResult.IsolationDurationDays
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import javax.inject.Inject

class EncounterDetectionViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val clock: Clock
) : ViewModel() {

    fun getIsolationDays() {
        viewModelScope.launch {
            val state = isolationStateMachine.readLogicalState()
            if (state.isActiveIsolation(clock)) {
                val isolationDays = isolationStateMachine.remainingDaysInIsolation().toInt()
                resultLiveData.postValue(IsolationDurationDays(isolationDays))
            }
        }
    }

    private val resultLiveData = MutableLiveData<ExposedNotificationResult>()

    fun isolationState(): LiveData<ExposedNotificationResult> = resultLiveData

    fun confirmConsent() {
        viewModelScope.launch {
            exposureNotificationRetryAlarmController.cancel()
            shouldShowEncounterDetectionActivityProvider.value = null
            analyticsEventProcessor.track(AcknowledgedStartOfIsolationDueToRiskyContact)

            resultLiveData.postValue(ConsentConfirmation)
        }
    }

    sealed class ExposedNotificationResult {

        data class IsolationDurationDays(val days: Int) : ExposedNotificationResult()

        object ConsentConfirmation : ExposedNotificationResult()
    }
}
