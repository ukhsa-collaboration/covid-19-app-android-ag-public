package uk.nhs.nhsx.covid19.android.app.di.viewmodel

import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgementCompletionActions
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.NoTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation

class MockTestResultViewModel : BaseTestResultViewModel() {
    companion object {
        var currentOptions = Options()
    }

    data class Options(
        val useMock: Boolean = false,
        val viewState: TestResultViewState = NegativeWontBeInIsolation,
        val actions: AcknowledgementCompletionActions = AcknowledgementCompletionActions(
            suggestBookTest = NoTest,
            shouldAllowKeySubmission = false
        ),
        val remainingDaysInIsolation: Int = 8
    )

    init {
        viewState.postValue(
            ViewState(
                currentOptions.viewState,
                currentOptions.remainingDaysInIsolation,
                currentOptions.actions
            )
        )
    }

    override fun onActionButtonClicked() = navigationEventLiveData.postValue(Finish)
    override fun onBackPressed() = Unit
}
