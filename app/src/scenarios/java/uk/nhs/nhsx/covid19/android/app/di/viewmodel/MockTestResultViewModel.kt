package uk.nhs.nhsx.covid19.android.app.di.viewmodel

import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation

class MockTestResultViewModel : BaseTestResultViewModel() {
    companion object {
        var currentOptions = Options()
    }

    data class Options(
        val useMock: Boolean = false,
        val viewState: TestResultViewState = NegativeWontBeInIsolation,
        val remainingDaysInIsolation: Int = 8
    )

    override fun onCreate() =
        viewState.postValue(ViewState(currentOptions.viewState, currentOptions.remainingDaysInIsolation))

    override fun onActionButtonClicked() = navigationEventLiveData.postValue(Finish)
    override fun onBackPressed() = Unit
}
