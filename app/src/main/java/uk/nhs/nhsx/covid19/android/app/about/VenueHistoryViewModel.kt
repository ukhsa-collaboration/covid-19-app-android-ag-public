package uk.nhs.nhsx.covid19.android.app.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsViewAdapter.VenueVisitEntry
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsViewAdapter.VenueVisitEntry.VenueVisitEntryHeader
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsViewAdapter.VenueVisitEntry.VenueVisitEntryItem
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.ZoneId
import javax.inject.Inject

class VenueHistoryViewModel @Inject constructor(
    private val venuesStorage: VisitedVenuesStorage,
) : ViewModel() {

    private val venueHistoryStateLiveData = MutableLiveData<VenueHistoryState>()
    fun venueHistoryState(): LiveData<VenueHistoryState> = venueHistoryStateLiveData

    private val venueVisitsEditModeChangedLiveData: MutableLiveData<Boolean> = SingleLiveEvent()
    fun venueVisitsEditModeChanged(): LiveData<Boolean> = venueVisitsEditModeChangedLiveData

    fun onResume() {
        viewModelScope.launch {
            val updatedViewState = VenueHistoryState(
                venueVisitEntries = getClusteredVenueVisits(),
                isInEditMode = isInEditMode(),
                confirmDeleteVenueVisit = venueHistoryStateLiveData.value?.confirmDeleteVenueVisit
            )
            if (venueHistoryStateLiveData.value != updatedViewState) {
                venueHistoryStateLiveData.postValue(updatedViewState)
            }
        }
    }

    private suspend fun getClusteredVenueVisits(): List<VenueVisitEntry> =
        venuesStorage.getVisits()
            .sortedWith(
                compareByDescending<VenueVisit> { it.from }
                    .thenBy { it.venue.organizationPartName }
            )
            .groupBy { item -> item.from.toLocalDate(ZoneId.systemDefault()) }
            .flatMap { (key, values) ->
                listOf(VenueVisitEntryHeader(key)).plus(
                    values.map {
                        VenueVisitEntryItem(
                            it
                        )
                    }
                )
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
            venueHistoryStateLiveData.postValue(
                venueHistoryStateLiveData.value!!.copy(
                    venueVisitEntries = getClusteredVenueVisits(),
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
        viewModelScope.launch {
            val toggleIsInEditMode = !venueHistoryStateLiveData.value!!.isInEditMode
            venueHistoryStateLiveData.postValue(
                venueHistoryStateLiveData.value!!.copy(
                    isInEditMode = toggleIsInEditMode
                )
            )
            venueVisitsEditModeChangedLiveData.postValue(toggleIsInEditMode)
        }
    }

    data class VenueHistoryState(
        val venueVisitEntries: List<VenueVisitEntry>,
        val isInEditMode: Boolean,
        val confirmDeleteVenueVisit: ConfirmDeleteVenueVisit? = null
    )

    data class ConfirmDeleteVenueVisit(val venueVisit: VenueVisit)
}
