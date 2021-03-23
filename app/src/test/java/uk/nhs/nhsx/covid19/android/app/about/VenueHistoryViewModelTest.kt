package uk.nhs.nhsx.covid19.android.app.about

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.about.VenueHistoryViewModel.VenueHistoryState
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsViewAdapter.VenueVisitEntry.VenueVisitEntryHeader
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsViewAdapter.VenueVisitEntry.VenueVisitEntryItem
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import java.time.Instant
import java.time.LocalDate

class VenueHistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val venuesStorage = mockk<VisitedVenuesStorage>(relaxed = true)

    private val testSubject = VenueHistoryViewModel(
        venuesStorage,
    )

    private val venueHistoryStateObserver = mockk<Observer<VenueHistoryState>>(relaxed = true)
    private val venueVisitsEditModeChangedObserver = mockk<Observer<Boolean>>(relaxed = true)

    @Before
    fun setUp() {
        testSubject.venueHistoryState().observeForever(venueHistoryStateObserver)
        testSubject.venueVisitsEditModeChanged().observeForever(venueVisitsEditModeChangedObserver)

        coEvery { venuesStorage.getVisits() } returns listOf()
    }

    @Test
    fun `onResume triggers view state emission`() = runBlocking {
        testSubject.onResume()

        verify { venueHistoryStateObserver.onChanged(expectedInitialState) }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
    }

    @Test
    fun `onResume with no changes to view state does not trigger view state emission`() = runBlocking {
        testSubject.onResume()
        testSubject.onResume()

        verify(exactly = 1) { venueHistoryStateObserver.onChanged(any()) }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
    }

    @Test
    fun `delete single venue visit removes it from storage`() = runBlocking {
        val venueVisit = VenueVisit(
            venue = Venue("1", "A"),
            from = Instant.parse("1970-01-01T18:00:00Z"),
            to = Instant.parse("1970-01-01T20:00:00Z")
        )

        testSubject.onResume()

        testSubject.deleteVenueVisit(venueVisit)

        coVerifyOrder {
            venueHistoryStateObserver.onChanged(expectedInitialState)
            venuesStorage.removeVenueVisit(venueVisit)
            venueHistoryStateObserver.onChanged(
                expectedInitialState.copy(
                    venueVisitEntries = listOf(),
                    isInEditMode = true,
                    confirmDeleteVenueVisit = null
                )
            )
        }
        verify(exactly = 0) { venueVisitsEditModeChangedObserver.onChanged(any()) }
    }

    @Test
    fun `clicking edit and done changes delete state`() {
        testSubject.onResume()
        testSubject.onEditVenueVisitClicked()
        testSubject.onEditVenueVisitClicked()

        coVerifyOrder {
            venueHistoryStateObserver.onChanged(expectedInitialState)
            venueHistoryStateObserver.onChanged(
                expectedInitialState.copy(
                    venueVisitEntries = listOf(),
                    isInEditMode = true,
                    confirmDeleteVenueVisit = null
                )
            )
            venueVisitsEditModeChangedObserver.onChanged(true)
            venueHistoryStateObserver.onChanged(
                expectedInitialState.copy(
                    venueVisitEntries = listOf(),
                    isInEditMode = false,
                    confirmDeleteVenueVisit = null
                )
            )
            venueVisitsEditModeChangedObserver.onChanged(false)
        }
    }

    @Test
    fun `list of venue visits is sorted correctly`() {
        val venueA = VenueVisit(
            venue = Venue("1", "A"),
            from = Instant.parse("1970-01-02T18:00:00Z"),
            to = Instant.parse("1970-01-02T20:00:00Z")
        )
        val venueB = VenueVisit(
            venue = Venue("1", "B"),
            from = Instant.parse("1970-01-02T13:00:00Z"),
            to = Instant.parse("1970-01-02T16:00:00Z")
        )
        val venueC = VenueVisit(
            venue = Venue("1", "C"),
            from = Instant.parse("1970-01-01T12:00:00Z"),
            to = Instant.parse("1970-01-01T14:00:00Z")
        )
        val venueD = VenueVisit(
            venue = Venue("1", "D"),
            from = Instant.parse("1970-01-01T12:00:00Z"),
            to = Instant.parse("1970-01-01T14:00:00Z")
        )

        coEvery { venuesStorage.getVisits() } returns listOf(venueD, venueC, venueB, venueA)

        testSubject.onResume()

        val expectedVenueVisits = listOf(
            VenueVisitEntryHeader(LocalDate.parse("1970-01-02")),
            VenueVisitEntryItem(venueA),
            VenueVisitEntryItem(venueB),
            VenueVisitEntryHeader(LocalDate.parse("1970-01-01")),
            VenueVisitEntryItem(venueC),
            VenueVisitEntryItem(venueD)
        )

        verify {
            venueHistoryStateObserver.onChanged(
                expectedInitialState.copy(
                    venueVisitEntries = expectedVenueVisits,
                    isInEditMode = false
                )
            )
        }
    }

    private val expectedInitialState =
        VenueHistoryState(
            venueVisitEntries = listOf(),
            isInEditMode = false,
            confirmDeleteVenueVisit = null
        )
}
