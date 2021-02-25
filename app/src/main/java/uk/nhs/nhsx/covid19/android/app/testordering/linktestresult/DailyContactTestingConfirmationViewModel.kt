package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class DailyContactTestingConfirmationViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine
) : ViewModel() {

    private val showDialogLiveData: MutableLiveData<Boolean> = MutableLiveData()
    fun showDialog(): LiveData<Boolean> = showDialogLiveData

    private val confirmedDailyContactTestingLiveData = SingleLiveEvent<Unit>()
    fun confirmedDailyContactTesting(): LiveData<Unit> = confirmedDailyContactTestingLiveData

    fun onOpenDialogClicked() {
        showDialogLiveData.postValue(true)
    }

    fun onDialogDismissed() {
        showDialogLiveData.postValue(false)
    }

    fun onOptInToDailyContactTestingConfirmed() {
        isolationStateMachine.optInToDailyContactTesting()
        confirmedDailyContactTestingLiveData.postCall()
    }
}
