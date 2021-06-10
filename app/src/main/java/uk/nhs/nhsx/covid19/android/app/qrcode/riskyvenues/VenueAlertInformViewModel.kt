package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformViewModel.ViewState.KnownVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertInformViewModel.ViewState.UnknownVisit
import javax.inject.Inject

class VenueAlertInformViewModel @Inject constructor(
    private val venuesStorage: VisitedVenuesStorage,
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
) : ViewModel() {
    private val venueVisitLiveData = MutableLiveData<ViewState>()
    fun venueVisitState(): LiveData<ViewState> = venueVisitLiveData

    fun updateVenueVisitState(venueId: String) {
        viewModelScope.launch {
            val venueVisit = venuesStorage.getVisitByVenueId(venueId)

            val state = if (venueVisit != null) {
                KnownVisit(venueVisit)
            } else {
                UnknownVisit
            }
            venueVisitLiveData.postValue(state)
        }
    }

    fun acknowledgeVenueAlert() {
        riskyVenueAlertProvider.riskyVenueAlert = null
    }

    sealed class ViewState {
        data class KnownVisit(val venue: VenueVisit) : ViewState()
        object UnknownVisit : ViewState()
    }
}
