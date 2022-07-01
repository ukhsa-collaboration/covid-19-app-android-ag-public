package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.NavigateToShareKeys
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.FollowUpTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.NoTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.RegularTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PlodWillContinueWithCurrentState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation

class TestResultViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val evaluateTestResultViewState = mockk<EvaluateTestResultViewState>(relaxed = true)
    private val acknowledgeTestResult = mockk<AcknowledgeTestResult>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>(relaxUnitFun = true) {
        coEvery { requirePostCodeDistrict() } returns PostCodeDistrict.ENGLAND
    }

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val navigationObserver = mockk<Observer<NavigationEvent>>(relaxed = true)
    private val localAuthorityPostCodeObserver = mockk<Observer<Unit>>(relaxed = true)

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
        expectedViewStatesWithButtonActions.forEach { (viewStateAndActions, expectedNavigationEvent) ->
            every { evaluateTestResultViewState() } returns ViewState(
                viewStateAndActions.first,
                remainingDaysInIsolation = 0,
                viewStateAndActions.second
            )

            createTestSubject()

            testSubject.onActionButtonClicked()

            verify { acknowledgeTestResult() }
            println("View state: $viewStateAndActions expectedNavigationEvent: $expectedNavigationEvent")
            verify { navigationObserver.onChanged(expectedNavigationEvent) }

            if (expectedNavigationEvent is NavigateToShareKeys) {
                verify { analyticsEventProcessor.track(AskedToShareExposureKeysInTheInitialFlow) }
            }
        }
    }

    private val expectedBookFollowUpTest = true

    private val expectedViewStatesWithButtonActions = mapOf(
        (NegativeNotInIsolation to noKeysNoTest) to Finish,
        (NegativeWillBeInIsolation to noKeysNoTest) to Finish,
        (NegativeWontBeInIsolation to noKeysNoTest) to Finish,
        (PositiveContinueIsolationNoChange to noKeysNoTest) to Finish,
        (NegativeAfterPositiveOrSymptomaticWillBeInIsolation to noKeysNoTest) to Finish,
        (PlodWillContinueWithCurrentState to noKeysNoTest) to Finish,
        (Ignore to noKeysNoTest) to Finish,
        (VoidNotInIsolation to noKeysNonFollowUpTest) to Finish,
        (VoidWillBeInIsolation to noKeysNonFollowUpTest) to Finish,
        (PositiveWillBeInIsolation to keysFollowUpTest) to NavigateToShareKeys(expectedBookFollowUpTest),
        (PositiveContinueIsolation to keysFollowUpTest) to NavigateToShareKeys(expectedBookFollowUpTest),
        (PositiveWontBeInIsolation to keysFollowUpTest) to NavigateToShareKeys(expectedBookFollowUpTest)
    )

    private fun createTestSubject() {
        testSubject = TestResultViewModel(
            evaluateTestResultViewState,
            acknowledgeTestResult,
            analyticsEventProcessor,
            localAuthorityPostCodeProvider
        )
        testSubject.fetchCountry()
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigationEvent().observeForever(navigationObserver)
    }

    companion object {
        val noKeysNoTest = AcknowledgementCompletionActions(suggestBookTest = NoTest, shouldAllowKeySubmission = false)
        val noKeysNonFollowUpTest =
            AcknowledgementCompletionActions(suggestBookTest = RegularTest, shouldAllowKeySubmission = false)
        val keysFollowUpTest =
            AcknowledgementCompletionActions(suggestBookTest = FollowUpTest, shouldAllowKeySubmission = true)
    }
}
