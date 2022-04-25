package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubViewModel.NavigationTarget

class GuidanceHubViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private lateinit var viewModel: GuidanceHubViewModel

    @Before
    fun setUp() {
        viewModel = GuidanceHubViewModel()
        viewModel.navigationTarget().observeForever(navigationTargetObserver)
    }

    @Test
    fun `when guidance for england item is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.EnglandGuidance

        viewModel.itemForEnglandGuidanceClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when check symptoms is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.CheckSymptomsGuidance

        viewModel.itemCheckSymptomsGuidanceClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when latest guidance is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.LatestGuidance

        viewModel.itemLatestGuidanceClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when positive test result is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.PositiveTestResultGuidance

        viewModel.itemPositiveTestResultGuidanceClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when travelling abroad is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.TravellingAbroadGuidance

        viewModel.itemTravellingAbroadGuidanceClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when SSP guidance is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.CheckSSPGuidance

        viewModel.itemCheckSSPGuidanceClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when enquiries is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.CovidEnquiryGuidance

        viewModel.itemCovidEnquiriesGuidanceClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }
}
