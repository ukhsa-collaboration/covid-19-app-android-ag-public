package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubWalesViewModel.NavigationTarget

class GuidanceHubWalesViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private lateinit var viewModel: GuidanceHubWalesViewModel

    @Before
    fun setUp() {
        viewModel = GuidanceHubWalesViewModel()
        viewModel.navigationTarget().observeForever(navigationTargetObserver)
    }

    @Test
    fun `when item one is clicked emit navigation event with url`() {
        val expectedNavigationTarget = NavigationTarget.ExternalLink(R.string.covid_guidance_hub_wales_button_one_url)

        viewModel.itemOneClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when item two is clicked emit navigation event with url`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_wales_button_two_url)

        viewModel.itemTwoClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when item three is clicked emit navigation event with url`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_wales_button_three_url)

        viewModel.itemThreeClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when item four clicked emit navigation event with url`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_wales_button_four_url)

        viewModel.itemFourClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when item five is clicked emit navigation event with url`() {
        val expectedNavigationTarget = NavigationTarget.ExternalLink(R.string.covid_guidance_hub_wales_button_five_url)

        viewModel.itemFiveClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when item six is clicked emit navigation event with url`() {
        val expectedNavigationTarget =
            NavigationTarget.ExternalLink(R.string.covid_guidance_hub_wales_button_six_url)

        viewModel.itemSixClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when item seven is clicked emit navigation event with url`() {
        val expectedNavigationTarget = NavigationTarget.ExternalLink(R.string.covid_guidance_hub_wales_button_seven_url)

        viewModel.itemSevenClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }

    @Test
    fun `when item eight is clicked emit navigation event with url`() {
        val expectedNavigationTarget = NavigationTarget.ExternalLink(R.string.covid_guidance_hub_wales_button_eight_url)

        viewModel.itemEightClicked()
        verify { navigationTargetObserver.onChanged(expectedNavigationTarget) }
    }
}
