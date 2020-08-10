package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow
import java.time.Instant
import kotlin.test.assertEquals

class VenueMatchFinderTest {

    private val visitedVenuesStorage = mockk<VisitedVenuesStorage>(relaxed = true)
    private val sut = VenueMatchFinder(visitedVenuesStorage)

    private val venueVisitNotRiskyInVenue1 = VenueVisit(
        venue = Venue(
            "1",
            organizationPartName = "Venue1"
        ),
        from = Instant.parse("2020-07-09T18:00:00.00Z"),
        to = Instant.parse("2020-07-10T01:00:00.00Z"),
        wasInRiskyList = false
    )

    private val venueVisitRiskyInVenue1 = VenueVisit(
        venue = Venue(
            "1",
            organizationPartName = "Venue1"
        ),
        from = Instant.parse("2020-07-08T09:00:00.00Z"),
        to = Instant.parse("2020-07-08T11:00:00.00Z"),
        wasInRiskyList = false
    )

    private val anotherVenueVisitRiskyInVenue1 = VenueVisit(
        venue = Venue(
            "1",
            organizationPartName = "Venue1"
        ),
        from = Instant.parse("2020-07-06T10:00:00.00Z"),
        to = Instant.parse("2020-07-09T23:00:00.00Z"),
        wasInRiskyList = false
    )

    private val venueVisitRiskyInVenue2AndWasInRiskyList = VenueVisit(
        venue = Venue(
            "2",
            organizationPartName = "Venue2"
        ),
        from = Instant.parse("2020-07-08T18:00:00.00Z"),
        to = Instant.parse("2020-07-08T21:00:00.00Z"),
        wasInRiskyList = true
    )

    private val venueVisitRiskyInVenue2 = VenueVisit(
        venue = Venue(
            "2",
            organizationPartName = "Venue2"
        ),
        from = Instant.parse("2020-07-08T18:00:00.00Z"),
        to = Instant.parse("2020-07-08T21:00:00.00Z"),
        wasInRiskyList = false
    )

    private val riskyVenues = listOf(
        RiskyVenue(
            "1",
            RiskyWindow(
                from = Instant.parse("2020-07-08T10:00:00.00Z"),
                to = Instant.parse("2020-07-08T12:00:00.00Z")
            )
        ),
        RiskyVenue(
            "2",
            RiskyWindow(
                from = Instant.parse("2020-07-07T20:00:00.00Z"),
                to = Instant.parse("2020-07-09T20:00:00.00Z")
            )
        )
    )

    @Test
    fun `calls getStoredVenues`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf()

        sut.findMatches(riskyVenues)

        coVerify { visitedVenuesStorage.getVisits() }
    }

    @Test
    fun `return empty list when no risky venues`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf(venueVisitNotRiskyInVenue1)

        val matches = sut.findMatches(listOf())

        assertEquals(0, matches.size)
    }

    @Test
    fun `return empty list when no venue visits`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf()

        val matches = sut.findMatches(riskyVenues)

        assertEquals(0, matches.size)
    }

    @Test
    fun `return empty list when no risky venues and no venue visits`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf()

        val matches = sut.findMatches(listOf())

        assertEquals(0, matches.size)
    }

    @Test
    fun `returns match when venue was visited in risky window`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf(venueVisitRiskyInVenue1)

        val matches = sut.findMatches(riskyVenues)

        assertEquals(1, matches.size)
        assertEquals("1", matches[0])
    }

    @Test
    fun `returns no match when venue was visited outside of risky window`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf(venueVisitNotRiskyInVenue1)

        val matches = sut.findMatches(riskyVenues)

        assertEquals(0, matches.size)
    }

    @Test
    fun `returns no match when visit is in risky window but was previously flagged as risky`() =
        runBlocking {
            coEvery { visitedVenuesStorage.getVisits() } returns listOf(
                venueVisitRiskyInVenue2AndWasInRiskyList
            )

            val matches = sut.findMatches(riskyVenues)

            assertEquals(0, matches.size)
        }

    @Test
    fun `returns matches when venue visit was in risky window`() = runBlocking {
        val venueVisits = listOf(venueVisitRiskyInVenue2)

        coEvery { visitedVenuesStorage.getVisits() } returns venueVisits

        val matches = sut.findMatches(riskyVenues)

        assertEquals(1, matches.size)
        assertEquals("2", matches[0])
    }

    @Test
    fun `return one match even if more than one visit in same venue was risky`() = runBlocking {
        val venueVisits = listOf(venueVisitRiskyInVenue1, anotherVenueVisitRiskyInVenue1)

        coEvery { visitedVenuesStorage.getVisits() } returns venueVisits

        val matches = sut.findMatches(riskyVenues)

        assertEquals(1, matches.size)
        assertEquals("1", matches[0])
    }
}
