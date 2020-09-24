package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionViewModel.ExposedNotificationResult.ConsentConfirmation
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionViewModel.ExposedNotificationResult.IsolationDurationDays
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.remainingDaysInIsolation
import javax.inject.Inject

class EncounterDetectionViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val inbox: UserInbox
) : ViewModel() {

    fun getIsolationDays() {
        viewModelScope.launch {
            val state = isolationStateMachine.readState()
            if (state is Isolation) {
                val isolationDays = isolationStateMachine.remainingDaysInIsolation().toInt()
                resultLiveData.postValue(IsolationDurationDays(isolationDays))
            }
        }
    }

    private val resultLiveData = MutableLiveData<ExposedNotificationResult>()

    fun isolationState(): LiveData<ExposedNotificationResult> = resultLiveData

    fun confirmConsent() {
        viewModelScope.launch {
            inbox.clearItem(ShowEncounterDetection)
            resultLiveData.postValue(ConsentConfirmation)
        }
    }

    sealed class ExposedNotificationResult {

        data class IsolationDurationDays(val days: Int) : ExposedNotificationResult()

        object ConsentConfirmation : ExposedNotificationResult()
    }
}
