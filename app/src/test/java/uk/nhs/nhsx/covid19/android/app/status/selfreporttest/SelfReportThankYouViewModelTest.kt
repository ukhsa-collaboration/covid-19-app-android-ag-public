package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.NavigationTarget.Advice

class SelfReportThankYouViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    @Test
    fun `on click continue in SuccessfullySharedKeysAndNoNeedToReportTest flow successfully navigates to Advice flow`() {
        val testSubject = SelfReportThankYouViewModel(sharingSuccessful = true, hasReported = true)
        testSubject.navigate().observeForever(navigationStateObserver)
        testSubject.onClickContinue()

        verify { navigationStateObserver.onChanged(Advice(hasReported = true)) }
    }

    @Test
    fun `on click continue in SuccessfullySharedKeysAndNHSTestNotReported flow successfully navigates to Advice flow`() {
        val testSubject = SelfReportThankYouViewModel(sharingSuccessful = true, hasReported = false)
        testSubject.navigate().observeForever(navigationStateObserver)
        testSubject.onClickContinue()

        verify { navigationStateObserver.onChanged(Advice(hasReported = false)) }
    }

    @Test
    fun `on click continue in UnsuccessfullySharedKeysAndNoNeedToReportTest flow successfully navigates to Advice flow`() {
        val testSubject = SelfReportThankYouViewModel(sharingSuccessful = false, hasReported = true)
        testSubject.navigate().observeForever(navigationStateObserver)
        testSubject.onClickContinue()

        verify { navigationStateObserver.onChanged(Advice(hasReported = true)) }
    }

    @Test
    fun `on click continue in UnsuccessfullySharedKeysAndNHSTestNotReported flow successfully navigates to Advice flow`() {
        val testSubject = SelfReportThankYouViewModel(sharingSuccessful = false, hasReported = false)
        testSubject.navigate().observeForever(navigationStateObserver)
        testSubject.onClickContinue()

        verify { navigationStateObserver.onChanged(Advice(hasReported = false)) }
    }

    @Test
    fun `check that SuccessfullySharedKeysAndNoNeedToReportTest flow is in correct view state according to answers`() {
        val testSubject = SelfReportThankYouViewModel(sharingSuccessful = true, hasReported = true)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(SuccessfullySharedKeysAndNoNeedToReportTest)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `check that SuccessfullySharedKeysAndNHSTestNotReported flow is in correct view state according to answers`() {
        val testSubject = SelfReportThankYouViewModel(sharingSuccessful = true, hasReported = false)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(SuccessfullySharedKeysAndNHSTestNotReported)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `check that UnsuccessfullySharedKeysAndNoNeedToReportTest flow is in correct view state according to answers`() {
        val testSubject = SelfReportThankYouViewModel(sharingSuccessful = false, hasReported = true)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(UnsuccessfullySharedKeysAndNoNeedToReportTest)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `check that UnsuccessfullySharedKeysAndNHSTestNotReported flow is in correct view state according to answers`() {
        val testSubject = SelfReportThankYouViewModel(sharingSuccessful = false, hasReported = false)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(UnsuccessfullySharedKeysAndNHSTestNotReported)
        verify { viewStateObserver.onChanged(expectedState) }
    }
}
