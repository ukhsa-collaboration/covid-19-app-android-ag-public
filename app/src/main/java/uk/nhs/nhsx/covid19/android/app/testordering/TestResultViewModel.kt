package uk.nhs.nhsx.covid19.android.app.testordering

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.OrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ShareKeys
import javax.inject.Inject

class TestResultViewModel @Inject constructor(
    evaluateTestResultViewState: EvaluateTestResultViewState,
    private val acknowledgeTestResult: AcknowledgeTestResult,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : BaseTestResultViewModel() {
    private var wasAcknowledged = false

    init {
        viewState.postValue(evaluateTestResultViewState())
    }

    override fun onActionButtonClicked() {
        acknowledgeTestResultIfNeeded()

        val navigationEvent = getNavigationEvent()

        if (navigationEvent != null) {
            navigationEventLiveData.postValue(navigationEvent)
        } else {
            Timber.d("Unexpected button action ${viewState.value?.mainState?.buttonAction}")
        }
    }

    private fun getNavigationEvent(): NavigationEvent? =
        when (val buttonAction = viewState.value?.mainState?.buttonAction) {
            Finish -> NavigationEvent.Finish
            is ShareKeys -> {
                analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow)
                NavigationEvent.NavigateToShareKeys(buttonAction.bookFollowUpTest)
            }
            OrderTest -> NavigationEvent.NavigateToOrderTest
            else -> null
        }

    override fun onBackPressed() {
        acknowledgeTestResultIfNeeded()
    }

    private fun acknowledgeTestResultIfNeeded() {
        if (wasAcknowledged) {
            return
        }
        wasAcknowledged = true
        acknowledgeTestResult()
    }
}
