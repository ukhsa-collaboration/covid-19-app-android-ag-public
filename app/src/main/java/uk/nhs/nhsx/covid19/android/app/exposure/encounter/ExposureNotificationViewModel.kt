package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ExposureNotificationViewModel @Inject constructor(
    private val getRiskyContactEncounterDate: GetRiskyContactEncounterDate,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock,
) : ViewModel() {
    private val viewStateLiveData = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> = viewStateLiveData

    private val finishActivityLiveData = SingleLiveEvent<Void>()
    val finishActivity: LiveData<Void> = finishActivityLiveData

    fun updateViewState() {
        val encounterDate = getRiskyContactEncounterDate()

        if (encounterDate == null) {
            Timber.e("Could not get encounter date")
            finishActivityLiveData.postCall()
            return
        }

        viewStateLiveData.postValue(
            ViewState(
                encounterDate = encounterDate,
                shouldShowTestingAndIsolationAdvice = !isolationStateMachine.readLogicalState().isActiveIndexCase(clock)
            )
        )
    }

    data class ViewState(
        val encounterDate: LocalDate,
        val shouldShowTestingAndIsolationAdvice: Boolean
    )
}
