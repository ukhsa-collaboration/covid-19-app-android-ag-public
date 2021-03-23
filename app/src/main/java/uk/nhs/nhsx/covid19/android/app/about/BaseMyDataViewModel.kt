package uk.nhs.nhsx.covid19.android.app.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import java.time.Instant
import java.time.LocalDate

abstract class BaseMyDataViewModel() : ViewModel() {
    protected val myDataStateLiveData = MutableLiveData<MyDataState>()
    fun myDataState(): LiveData<MyDataState> = myDataStateLiveData

    abstract fun onResume()

    protected abstract fun getIsolationState(): IsolationState?
    protected abstract fun getLastRiskyVenueVisitDate(): LocalDate?
    protected abstract fun getDailyContactTestingOptInDateForIsolation(isolation: Isolation): LocalDate?

    data class MyDataState(
        val isolationState: IsolationState?,
        val lastRiskyVenueVisitDate: LocalDate?,
        val acknowledgedTestResult: AcknowledgedTestResult?,
    )

    data class IsolationState(
        val lastDayOfIsolation: LocalDate? = null,
        val contactCaseEncounterDate: Instant? = null,
        val contactCaseNotificationDate: Instant? = null,
        val indexCaseSymptomOnsetDate: LocalDate? = null,
        val dailyContactTestingOptInDate: LocalDate? = null
    )
}
