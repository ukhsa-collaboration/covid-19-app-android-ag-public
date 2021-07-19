package uk.nhs.nhsx.covid19.android.app.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class VenueHistoryViewModel @Inject constructor(
    private val venuesStorage: VisitedVenuesStorage,
    private val clusterVenueVisits: ClusterVenueVisits,
) : ViewModel() {

    private val venueHistoryStateLiveData = MutableLiveData<VenueHistoryState>()
    fun venueHistoryState(): LiveData<VenueHistoryState> = venueHistoryStateLiveData

    private val venueVisitsEditModeChangedLiveData: MutableLiveData<Boolean> = SingleLiveEvent()
    fun venueVisitsEditModeChanged(): LiveData<Boolean> = venueVisitsEditModeChangedLiveData

    fun onResume() {
        viewModelScope.launch {
            val venueVisitHistory = venuesStorage.getVisits().map { VenueVisitHistory(it) }
            val updatedViewState = VenueHistoryState(
                venueVisitEntries = clusterVenueVisits(venueVisitHistory),
                isInEditMode = isInEditMode(),
                confirmDeleteVenueVisit = venueHistoryStateLiveData.value?.confirmDeleteVenueVisit
            )
            if (venueHistoryStateLiveData.value != updatedViewState) {
                venueHistoryStateLiveData.postValue(updatedViewState)
            }
        }
    }

    private fun isInEditMode() = venueHistoryStateLiveData.value?.isInEditMode ?: false

    fun onDeleteVenueVisitDataClicked(venueVisit: VenueVisit) {
        venueHistoryStateLiveData.postValue(
            venueHistoryStateLiveData.value!!.copy(
                confirmDeleteVenueVisit = ConfirmDeleteVenueVisit(venueVisit)
            )
        )
    }

    fun deleteVenueVisit(venueVisit: VenueVisit) {
        viewModelScope.launch {
            venuesStorage.removeVenueVisit(venueVisit)
            val venueVisitHistory = venuesStorage.getVisits().map { VenueVisitHistory(it) }
            venueHistoryStateLiveData.postValue(
                venueHistoryStateLiveData.value!!.copy(
                    venueVisitEntries = clusterVenueVisits(venueVisitHistory),
                    isInEditMode = true,
                    confirmDeleteVenueVisit = null
                )
            )
        }
    }

    fun onDialogDismissed() {
        venueHistoryStateLiveData.postValue(venueHistoryStateLiveData.value!!.copy(confirmDeleteVenueVisit = null))
    }

    fun onEditVenueVisitClicked() {
        val toggleIsInEditMode = !venueHistoryStateLiveData.value!!.isInEditMode
        venueHistoryStateLiveData.postValue(
            venueHistoryStateLiveData.value!!.copy(
                isInEditMode = toggleIsInEditMode
            )
        )
        venueVisitsEditModeChangedLiveData.postValue(toggleIsInEditMode)
    }

    data class VenueHistoryState(
        val venueVisitEntries: List<VenueVisitListItem>,
        val isInEditMode: Boolean,
        val confirmDeleteVenueVisit: ConfirmDeleteVenueVisit? = null
    )

    data class ConfirmDeleteVenueVisit(val venueVisit: VenueVisit)
}
