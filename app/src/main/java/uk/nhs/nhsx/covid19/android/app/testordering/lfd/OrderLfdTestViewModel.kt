package uk.nhs.nhsx.covid19.android.app.testordering.lfd

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasLfdTestM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedLfdTestOrderingM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.NavigationTarget.OrderTest
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class OrderLfdTestViewModel @Inject constructor(
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val districtAreaStringProvider: DistrictAreaStringProvider
) : ViewModel() {

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

    fun onOrderTestClicked() {
        analyticsEventProcessor.track(SelectedLfdTestOrderingM2Journey)

        viewModelScope.launch {
            val url = districtAreaStringProvider.provide(R.string.url_nhs_get_tested)
            navigationTarget.postValue(OrderTest(url))
        }
    }

    fun onAlreadyHaveTestKitClicked() {
        analyticsEventProcessor.track(SelectedHasLfdTestM2Journey)
        navigationTarget.postValue(Home)
    }

    fun onReturnedFromTestOrdering() {
        navigationTarget.postValue(Home)
    }
}

sealed class NavigationTarget {
    data class OrderTest(val url: Int) : NavigationTarget()
    object Home : NavigationTarget()
}
