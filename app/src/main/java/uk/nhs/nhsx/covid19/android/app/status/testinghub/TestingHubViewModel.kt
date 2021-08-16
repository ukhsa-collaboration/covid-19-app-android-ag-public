package uk.nhs.nhsx.covid19.android.app.status.testinghub

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState.LfdTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState.PcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget.BookPcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget.OrderLfdTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget.SymptomsAfterRiskyVenue
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import javax.inject.Inject

class TestingHubViewModel @Inject constructor(
    private val districtAreaStringProvider: DistrictAreaStringProvider,
    private val evaluateBookTestNavigation: EvaluateBookTestNavigation,
    private val canBookPcrTest: CanBookPcrTest,
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val navigationTarget = SingleLiveEvent<NavigationTarget>()
    fun navigationTarget(): LiveData<NavigationTarget> = navigationTarget

    fun onResume() {
        viewStateLiveData.postValue(ViewState(bookTestButtonState = evaluateBookTestButtonState()))
    }

    private fun evaluateBookTestButtonState(): BookTestButtonState =
        if (canBookPcrTest()) PcrTest else LfdTest

    fun onOrderLfdTestClicked() {
        viewModelScope.launch {
            val urlResId = districtAreaStringProvider.provide(R.string.url_nhs_get_tested)
            navigationTarget.postValue(OrderLfdTest(urlResId))
        }
    }

    fun onBookPcrTestClicked() {
        navigationTarget.postValue(evaluateBookTestNavigation().toTestingHubNavigationTarget())
    }

    private fun EvaluateBookTestNavigation.NavigationTarget.toTestingHubNavigationTarget(): NavigationTarget =
        when (this) {
            EvaluateBookTestNavigation.NavigationTarget.BookPcrTest -> BookPcrTest
            EvaluateBookTestNavigation.NavigationTarget.SymptomsAfterRiskyVenue -> SymptomsAfterRiskyVenue
        }

    data class ViewState(
        val bookTestButtonState: BookTestButtonState
    )

    sealed class BookTestButtonState {
        object PcrTest : BookTestButtonState()
        object LfdTest : BookTestButtonState()
    }

    sealed class NavigationTarget {
        object BookPcrTest : NavigationTarget()
        data class OrderLfdTest(@StringRes val urlResId: Int) : NavigationTarget()
        object SymptomsAfterRiskyVenue : NavigationTarget()
    }
}
