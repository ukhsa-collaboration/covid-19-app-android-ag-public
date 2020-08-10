package uk.nhs.nhsx.covid19.android.app.qrcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class VenueCheckInViewModel @Inject constructor(
    private val visitedVenuesStorage: VisitedVenuesStorage,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel() {

    private val visitRemovedResult = SingleLiveEvent<RemoveVisitResult>()
    fun getVisitRemovedResult(): LiveData<RemoveVisitResult> = visitRemovedResult

    fun removeLastVisit() {
        viewModelScope.launch {

            visitedVenuesStorage.removeLastVisit()
            visitRemovedResult.postValue(RemoveVisitResult)
            analyticsEventProcessor.track(CanceledCheckIn)
        }
    }
}

typealias RemoveVisitResult = Unit
