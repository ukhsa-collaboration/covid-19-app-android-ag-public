package uk.nhs.nhsx.covid19.android.app.status.testinghub

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget.BookATest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ShowFindOutAboutTesting.DoNotShow
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ShowFindOutAboutTesting.Show
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import javax.inject.Inject

class TestingHubViewModel @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val districtAreaStringProvider: DistrictAreaStringProvider,
    private val evaluateVenueAlertNavigation: EvaluateVenueAlertNavigation,
    private val clock: Clock,
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

    fun onResume() {
        viewModelScope.launch {
            val isolationState = isolationStateMachine.readLogicalState()
            viewStateLiveData.postValue(
                ViewState(
                    showBookTestButton = canOrderTest(isolationState),
                    showFindOutAboutTestingButton = shouldShowFindOutAboutTestingButton(isolationState)
                )
            )
        }
    }

    fun onBookATestClicked() {
        if (lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk()) {
            navigationTarget.postValue(evaluateVenueAlertNavigation())
        } else {
            navigationTarget.postValue(BookATest)
        }
    }

    private fun canOrderTest(isolationState: IsolationLogicalState) =
        lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() ||
            isolationState.isActiveIsolation(clock)

    private suspend fun shouldShowFindOutAboutTestingButton(isolationState: IsolationLogicalState) =
        if (!isolationState.isActiveIsolation(clock)) {
            Show(districtAreaStringProvider.provide(string.url_latest_advice))
        } else DoNotShow

    data class ViewState(
        val showBookTestButton: Boolean,
        val showFindOutAboutTestingButton: ShowFindOutAboutTesting,
    )

    sealed class ShowFindOutAboutTesting {
        data class Show(val urlResId: Int) : ShowFindOutAboutTesting()
        object DoNotShow : ShowFindOutAboutTesting()
    }
}
