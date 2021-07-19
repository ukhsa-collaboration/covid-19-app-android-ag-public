package uk.nhs.nhsx.covid19.android.app.testordering.lfd

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasLfdTestM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedLfdTestOrderingM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.NavigationTarget.OrderTest
import uk.nhs.nhsx.covid19.android.app.util.DistrictAreaStringProvider

class OrderLfdTestViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val districtAreaStringProvider = mockk<DistrictAreaStringProvider>(relaxUnitFun = true)
    private val expectedUrlResId = 0

    private val testSubject = OrderLfdTestViewModel(analyticsEventProcessor, districtAreaStringProvider)

    @Before
    fun setUp() {
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
        coEvery { districtAreaStringProvider.provide(any()) } returns expectedUrlResId
    }

    @Test
    fun `on orderTestButton clicked should navigate to order test screen`() {
        testSubject.onOrderTestClicked()

        verifyOrder {
            analyticsEventProcessor.track(SelectedLfdTestOrderingM2Journey)
            navigationTargetObserver.onChanged(OrderTest(expectedUrlResId))
        }
    }

    @Test
    fun `on alreadyHaveTestKitClicked clicked should navigate to home screen`() {
        testSubject.onAlreadyHaveTestKitClicked()

        verify {
            analyticsEventProcessor.track(SelectedHasLfdTestM2Journey)
            navigationTargetObserver.onChanged(Home)
        }
    }

    @Test
    fun `onReturnedFromTestOrdering should navigate to home screen`() {
        testSubject.onReturnedFromTestOrdering()

        verify { navigationTargetObserver.onChanged(Home) }
    }
}
