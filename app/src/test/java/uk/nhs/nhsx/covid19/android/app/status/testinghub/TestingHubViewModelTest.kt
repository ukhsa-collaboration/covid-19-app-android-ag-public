package uk.nhs.nhsx.covid19.android.app.status.testinghub

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState.LfdTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.BookTestButtonState.PcrTest
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.testinghub.TestingHubViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider

class TestingHubViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val districtAreaStringProvider = mockk<DistrictAreaStringProvider>()
    private val evaluateBookTestNavigation = mockk<EvaluateBookTestNavigation>()
    private val canBookPcrTest = mockk<CanBookPcrTest>()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private val testSubject = TestingHubViewModel(
        districtAreaStringProvider,
        evaluateBookTestNavigation,
        canBookPcrTest
    )

    private val expectedUrlResId = 123

    @Before
    fun setUp() {
        coEvery { districtAreaStringProvider.provide(any()) } returns expectedUrlResId
        coEvery { canBookPcrTest() } returns false

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
    }

    @Test
    fun `show book LFD test button when CanBookPcrTest returns false`() {
        testSubject.onResume()

        verify { viewStateObserver.onChanged(ViewState(bookTestButtonState = LfdTest)) }
    }

    @Test
    fun `show book PCR test button when CanBookPcrTest returns true`() {
        coEvery { canBookPcrTest() } returns true

        testSubject.onResume()

        verify { viewStateObserver.onChanged(ViewState(bookTestButtonState = PcrTest)) }
    }

    @Test
    fun `when order LFD test clicked then emit OrderLfdTest`() {
        testSubject.onOrderLfdTestClicked()

        val expectedNavigationTarget = NavigationTarget.OrderLfdTest(expectedUrlResId)

        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when book PCR test clicked then emit value returned by EvaluateBookTestNavigation`() {
        every { evaluateBookTestNavigation() } returns EvaluateBookTestNavigation.NavigationTarget.SymptomsAfterRiskyVenue

        testSubject.onBookPcrTestClicked()

        val expectedNavigationTarget = NavigationTarget.SymptomsAfterRiskyVenue

        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }
}
