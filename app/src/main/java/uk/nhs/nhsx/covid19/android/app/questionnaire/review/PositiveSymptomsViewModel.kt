package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.remainingDaysInIsolation
import javax.inject.Inject

class PositiveSymptomsViewModel @Inject constructor(
    private val stateMachine: IsolationStateMachine
) : ViewModel() {

    private val daysUntilExpirationLivaData: MutableLiveData<Long> = MutableLiveData()

    fun daysUntilExpiration(): LiveData<Long> = daysUntilExpirationLivaData

    fun calculateDaysUntilExpiration() {
        daysUntilExpirationLivaData.postValue(stateMachine.remainingDaysInIsolation())
    }
}
