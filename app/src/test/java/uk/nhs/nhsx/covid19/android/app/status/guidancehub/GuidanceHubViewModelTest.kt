package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
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
    fun `when england item one is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.ExternalLink(R.string.covid_guidance_hub_england_button_one_url)

        viewModel.itemOneClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when england item two is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.ExternalLink(R.string.covid_guidance_hub_england_button_two_url)

        viewModel.itemTwoClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when england item three is clicked emit navigation event`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_england_button_three_url)

        viewModel.itemThreeClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when england item four is clicked emit navigation event`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_england_button_four_url)

        viewModel.itemFourClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when england item five is clicked emit navigation event`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_england_button_five_url)

        viewModel.itemFiveClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when england item six is clicked emit navigation event`() {
        val expectedNavigationTarget = NavigationTarget.ExternalLink(R.string.covid_guidance_hub_england_button_six_url)

        viewModel.itemSixClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when england item seven is clicked emit navigation event`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_england_button_seven_url)

        viewModel.itemSevenClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when england item eight is clicked emit navigation event`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_england_button_eight_url)

        viewModel.itemEightClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }
}
