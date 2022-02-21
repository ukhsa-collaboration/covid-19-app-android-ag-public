package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.FollowUpTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.NoTest
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

        navigationEventLiveData.postValue(navigationEvent)
    }

    private fun getNavigationEvent(): NavigationEvent {
        val completionActions = viewState.value?.acknowledgementCompletionActions
        return when {
            completionActions?.shouldAllowKeySubmission == true -> {
                analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow)
                NavigationEvent.NavigateToShareKeys(bookFollowUpTest = completionActions.suggestBookTest == FollowUpTest)
            }
            completionActions?.suggestBookTest != NoTest -> {
                NavigationEvent.NavigateToOrderTest
            }
            else -> {
                NavigationEvent.Finish
            }
        }
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
