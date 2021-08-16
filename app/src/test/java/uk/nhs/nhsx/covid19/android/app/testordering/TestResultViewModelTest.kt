package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.NavigateToOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.NavigateToShareKeys
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ShareKeys
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PlodWillContinueWithCurrentState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation

class TestResultViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val evaluateTestResultViewState = mockk<EvaluateTestResultViewState>(relaxed = true)
    private val acknowledgeTestResult = mockk<AcknowledgeTestResult>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val navigationObserver = mockk<Observer<NavigationEvent>>(relaxed = true)

    private lateinit var testSubject: TestResultViewModel

    @Test
    fun `onActionButtonClicked acknowledges test result`() {
        createTestSubject()

        testSubject.onBackPressed()

        verify { acknowledgeTestResult() }
    }

    @Test
    fun `onBackPressed acknowledges test result`() {
        createTestSubject()

        testSubject.onBackPressed()

        verify { acknowledgeTestResult() }
    }

    @Test
    fun `invoking onBackPressed twice only acknowledges test result once`() {
        createTestSubject()

        testSubject.onBackPressed()
        testSubject.onBackPressed()

        verify(exactly = 1) { acknowledgeTestResult() }
    }

    @Test
    fun `onActionButtonClicked emit appropriate navigation event`() {
        expectedViewStatesWithButtonActions.forEach { (viewState, expectedNavigationEvent) ->
            every { evaluateTestResultViewState() } returns ViewState(viewState, remainingDaysInIsolation = 0)

            createTestSubject()

            testSubject.onActionButtonClicked()

            verify { acknowledgeTestResult() }
            verify { navigationObserver.onChanged(expectedNavigationEvent) }

            if (expectedNavigationEvent is NavigateToShareKeys) {
                verify { analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow) }
            }
        }
    }

    private val expectedBookFollowUpTest = true

    private val expectedViewStatesWithButtonActions = mapOf(
        NegativeNotInIsolation to Finish,
        NegativeWillBeInIsolation to Finish,
        NegativeWontBeInIsolation to Finish,
        PositiveContinueIsolationNoChange to Finish,
        NegativeAfterPositiveOrSymptomaticWillBeInIsolation to Finish,
        PlodWillContinueWithCurrentState to Finish,
        Ignore to Finish,
        PositiveWillBeInIsolationAndOrderTest to NavigateToOrderTest,
        VoidNotInIsolation to NavigateToOrderTest,
        VoidWillBeInIsolation to NavigateToOrderTest,
        PositiveWillBeInIsolation(ShareKeys(expectedBookFollowUpTest)) to NavigateToShareKeys(expectedBookFollowUpTest),
        PositiveContinueIsolation(ShareKeys(expectedBookFollowUpTest)) to NavigateToShareKeys(expectedBookFollowUpTest),
        PositiveWontBeInIsolation(ShareKeys(expectedBookFollowUpTest)) to NavigateToShareKeys(expectedBookFollowUpTest)
    )

    private fun createTestSubject() {
        testSubject = TestResultViewModel(evaluateTestResultViewState, acknowledgeTestResult, analyticsEventProcessor)
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigationEvent().observeForever(navigationObserver)
    }
}
