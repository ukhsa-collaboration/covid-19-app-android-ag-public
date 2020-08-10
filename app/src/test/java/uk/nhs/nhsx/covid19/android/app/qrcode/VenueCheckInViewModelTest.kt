package uk.nhs.nhsx.covid19.android.app.qrcode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage

class VenueCheckInViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val visitRemoveResult = mockk<Observer<RemoveVisitResult>>(relaxed = true)
    private val visitedVenuesStorage = mockk<VisitedVenuesStorage>(relaxUnitFun = true)
    private val analyticsManager = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val sut = VenueCheckInViewModel(visitedVenuesStorage, analyticsManager)

    @Test
    fun `calls visit removed live data`() {
        sut.getVisitRemovedResult().observeForever(visitRemoveResult)

        sut.removeLastVisit()

        verify { visitRemoveResult.onChanged(RemoveVisitResult) }
    }
}
