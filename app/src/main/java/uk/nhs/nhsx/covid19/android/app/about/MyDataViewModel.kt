package uk.nhs.nhsx.covid19.android.app.about

import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
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
        viewModelScope.launch {
            val updatedViewState = MyDataState(
                isolationState = getIsolationState(),
                lastRiskyVenueVisitDate = getLastRiskyVenueVisitDate(),
                acknowledgedTestResult = stateMachine.readState().indexInfo?.testResult
            )
            if (myDataStateLiveData.value != updatedViewState) {
                myDataStateLiveData.postValue(updatedViewState)
            }
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
                    indexCaseSymptomOnsetDate = ((isolationState.indexInfo as? IndexCase)?.isolationTrigger as? SelfAssessment)?.assumedOnsetDate,
                    dailyContactTestingOptInDate = getDailyContactTestingOptInDate(isolationState.contactCase)
                )
        }

    override fun getLastRiskyVenueVisitDate(): LocalDate? = lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue?.latestDate

    override fun getDailyContactTestingOptInDate(contactCase: ContactCase?): LocalDate? =
        if (RuntimeBehavior.isFeatureEnabled(DAILY_CONTACT_TESTING)) {
            contactCase?.dailyContactTestingOptInDate
        } else null
}
