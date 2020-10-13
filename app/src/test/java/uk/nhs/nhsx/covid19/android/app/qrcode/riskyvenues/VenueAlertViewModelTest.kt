package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertViewModel.ViewState.KnownVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueAlertViewModel.ViewState.UnknownVisit
import java.time.Instant

class VenueAlertViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val venuesStorage = mockk<VisitedVenuesStorage>()

    private val testSubject = VenueAlertViewModel(venuesStorage)

    private val venueVisitObserver = mockk<Observer<ViewState>>(relaxed = true)

    @Test
    fun `update venue visit state when there is venue visit with provided venue id`() = runBlocking {
        val venueId = "venueId"
        val venueVisit = VenueVisit(venue = Venue(venueId, ""), from = Instant.now(), to = Instant.now())

        coEvery { venuesStorage.getVisitByVenueId(venueId) } returns venueVisit

        testSubject.venueVisitState().observeForever(venueVisitObserver)

        testSubject.updateVenueVisitState(venueId)

        coVerify { venueVisitObserver.onChanged(KnownVisit(venueVisit)) }
    }

    @Test
    fun `update venue visit state when there is no venue visit with provided venue id`() = runBlocking {
        val venueId = "venueId"

        coEvery { venuesStorage.getVisitByVenueId(venueId) } returns null

        testSubject.venueVisitState().observeForever(venueVisitObserver)

        testSubject.updateVenueVisitState(venueId)

        coVerify { venueVisitObserver.onChanged(UnknownVisit) }
    }
}
