package uk.nhs.nhsx.covid19.android.app.about

import androidx.lifecycle.viewModelScope
import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import java.time.LocalDate
import javax.inject.Inject

class MyDataViewModel @Inject constructor(
    private val stateMachine: IsolationStateMachine,
    private val relevantTestResultProvider: RelevantTestResultProvider,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider
) : BaseMyDataViewModel() {

    override fun onResume() {
        viewModelScope.launch {
            val updatedViewState = MyDataState(
                isolationState = getIsolationState(),
                lastRiskyVenueVisitDate = getLastRiskyVenueVisitDate(),
                acknowledgedTestResult = relevantTestResultProvider.testResult
            )
            if (myDataStateLiveData.value != updatedViewState) {
                myDataStateLiveData.postValue(updatedViewState)
            }
        }
    }

    override fun getIsolationState(): IsolationState? =
        when (val isolationState = stateMachine.readState()) {
            is Default -> isolationState.previousIsolation?.toIsolationState(isActiveIsolation = false)
            is Isolation -> isolationState.toIsolationState(isActiveIsolation = true)
        }

    private fun Isolation?.toIsolationState(isActiveIsolation: Boolean): IsolationState? =
        if (this == null) null
        else IsolationState(
            lastDayOfIsolation = if (isActiveIsolation) lastDayOfIsolation else null,
            contactCaseEncounterDate = contactCase?.startDate,
            contactCaseNotificationDate = contactCase?.notificationDate,
            indexCaseSymptomOnsetDate = indexCase?.symptomsOnsetDate,
            dailyContactTestingOptInDate = getDailyContactTestingOptInDateForIsolation(this)
        )

    override fun getLastRiskyVenueVisitDate(): LocalDate? = lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue?.latestDate

    override fun getDailyContactTestingOptInDateForIsolation(isolation: Isolation): LocalDate? =
        if (RuntimeBehavior.isFeatureEnabled(DAILY_CONTACT_TESTING)) {
            isolation.contactCase?.dailyContactTestingOptInDate
        } else null
}
