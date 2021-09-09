package uk.nhs.nhsx.covid19.android.app.about.mydata

import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class MyDataViewModel @Inject constructor(
    private val stateMachine: IsolationStateMachine,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val clock: Clock
) : BaseMyDataViewModel() {

    override fun onResume() {
        val updatedViewState = MyDataState(
            isolationState = getIsolationState(),
            lastRiskyVenueVisitDate = getLastRiskyVenueVisitDate(),
            acknowledgedTestResult = stateMachine.readState().testResult
        )
        if (myDataStateLiveData.value != updatedViewState) {
            myDataStateLiveData.postValue(updatedViewState)
        }
    }

    override fun getIsolationState(): IsolationViewState? =
        when (val isolationState = stateMachine.readLogicalState()) {
            is NeverIsolating -> null
            is PossiblyIsolating ->
                IsolationViewState(
                    lastDayOfIsolation =
                    if (isolationState.hasExpired(clock)) null
                    else isolationState.lastDayOfIsolation,
                    contactCaseEncounterDate = isolationState.contactCase?.exposureDate,
                    contactCaseNotificationDate = isolationState.contactCase?.notificationDate,
                    indexCaseSymptomOnsetDate = (isolationState.indexInfo as? IndexCase)?.selfAssessment?.assumedOnsetDate,
                    optOutOfContactIsolationDate = isolationState.contactCase?.optOutOfContactIsolation?.date
                )
        }

    override fun getLastRiskyVenueVisitDate(): LocalDate? =
        lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue?.latestDate
}
