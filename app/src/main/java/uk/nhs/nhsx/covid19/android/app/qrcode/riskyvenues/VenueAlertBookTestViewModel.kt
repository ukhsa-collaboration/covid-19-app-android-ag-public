package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState.KnownVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertBookTestViewModel.ViewState.UnknownVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.BOOK_TEST
import javax.inject.Inject

class VenueAlertBookTestViewModel @Inject constructor(
    private val venuesStorage: VisitedVenuesStorage,
    private val userInbox: UserInbox
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

    fun acknowledgeVenueAlert(venueId: String) {
        userInbox.clearItem(ShowVenueAlert(venueId, BOOK_TEST))
    }

    sealed class ViewState {
        data class KnownVisit(val venue: VenueVisit) : ViewState()
        object UnknownVisit : ViewState()
    }
}
