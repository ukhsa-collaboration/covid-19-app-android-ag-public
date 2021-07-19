package uk.nhs.nhsx.covid19.android.app.di.viewmodel

import uk.nhs.nhsx.covid19.android.app.about.mydata.BaseMyDataViewModel
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import java.time.LocalDate

class MockMyDataViewModel : BaseMyDataViewModel() {
    companion object {
        var currentOptions = Options()
    }

    data class Options(
        val useMock: Boolean = false,
        val isolationViewState: IsolationViewState? = null,
        val lastRiskyVenueVisitDate: LocalDate? = null,
        val dailyContactTestingOptInDateForIsolation: LocalDate? = null,
        val acknowledgedTestResult: AcknowledgedTestResult? = null
    )

    override fun onResume() {
        val updatedViewState = MyDataState(
            isolationState = getIsolationState(),
            lastRiskyVenueVisitDate = getLastRiskyVenueVisitDate(),
            acknowledgedTestResult = getTestResult()
        )
        if (myDataStateLiveData.value != updatedViewState) {
            myDataStateLiveData.postValue(updatedViewState)
        }
    }

    override fun getIsolationState(): IsolationViewState? = currentOptions.isolationViewState

    override fun getLastRiskyVenueVisitDate(): LocalDate? = currentOptions.lastRiskyVenueVisitDate

    override fun getDailyContactTestingOptInDate(contactCase: ContactCase?): LocalDate? =
        currentOptions.dailyContactTestingOptInDateForIsolation

    private fun getTestResult(): AcknowledgedTestResult? = currentOptions.acknowledgedTestResult
}
