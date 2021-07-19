package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedTakeTestLaterM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedTakeTestM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.EvaluateVenueAlertNavigation.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState.KnownVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState.UnknownVisit
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class VenueAlertBookTestViewModel @Inject constructor(
    private val venuesStorage: VisitedVenuesStorage,
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
    private val evaluateVenueAlertNavigation: EvaluateVenueAlertNavigation,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel() {
    private val venueVisitLiveData = MutableLiveData<ViewState>()
    fun venueVisitState(): LiveData<ViewState> = venueVisitLiveData

    private val navigationEventLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationEvent(): LiveData<NavigationTarget> = navigationEventLiveData

    fun updateVenueVisitState(venueId: String) {
        viewModelScope.launch {
            val venueVisit = venuesStorage.getVisitByVenueId(venueId)

            val state = if (venueVisit != null) {
                KnownVisit(venueVisit)
            } else {
                acknowledgeVenueAlert()
                UnknownVisit
            }
            venueVisitLiveData.postValue(state)
        }
    }

    fun acknowledgeVenueAlert() {
        riskyVenueAlertProvider.riskyVenueAlert = null
    }

    fun onBookATestClicked() {
        analyticsEventProcessor.track(SelectedTakeTestM2Journey)
        acknowledgeVenueAlert()
        navigationEventLiveData.postValue(evaluateVenueAlertNavigation())
    }

    fun onReturnToHomeClicked() {
        analyticsEventProcessor.track(SelectedTakeTestLaterM2Journey)
        acknowledgeVenueAlert()
        navigationEventLiveData.postValue(Finish)
    }

    sealed class ViewState {
        data class KnownVisit(val venue: VenueVisit) : ViewState()
        object UnknownVisit : ViewState()
    }
}
