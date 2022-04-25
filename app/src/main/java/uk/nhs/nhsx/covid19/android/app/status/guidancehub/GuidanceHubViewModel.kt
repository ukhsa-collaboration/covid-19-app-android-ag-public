package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class GuidanceHubViewModel @Inject constructor() : ViewModel() {
    private val navigationTargetLiveData = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTargetLiveData

    fun itemForEnglandGuidanceClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.EnglandGuidance)
    }

    fun itemCheckSymptomsGuidanceClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.CheckSymptomsGuidance)
    }

    fun itemLatestGuidanceClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.LatestGuidance)
    }

    fun itemPositiveTestResultGuidanceClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.PositiveTestResultGuidance)
    }

    fun itemTravellingAbroadGuidanceClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.TravellingAbroadGuidance)
    }

    fun itemCheckSSPGuidanceClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.CheckSSPGuidance)
    }

    fun itemCovidEnquiriesGuidanceClicked() {
        navigationTargetLiveData.postValue(NavigationTarget.CovidEnquiryGuidance)
    }

    sealed class NavigationTarget {
        object EnglandGuidance : NavigationTarget()
        object CheckSymptomsGuidance : NavigationTarget()
        object LatestGuidance : NavigationTarget()
        object PositiveTestResultGuidance : NavigationTarget()
        object TravellingAbroadGuidance : NavigationTarget()
        object CheckSSPGuidance : NavigationTarget()
        object CovidEnquiryGuidance : NavigationTarget()
    }
}
