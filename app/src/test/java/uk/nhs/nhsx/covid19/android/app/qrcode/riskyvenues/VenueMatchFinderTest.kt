package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VenueMatchFinder.Interval
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyWindow
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
            ),
            messageType = INFORM
        ),
        RiskyVenue(
            "2",
            RiskyWindow(
                from = Instant.parse("2020-07-07T20:00:00.00Z"),
                to = Instant.parse("2020-07-09T20:00:00.00Z")
            ),
            messageType = INFORM
        )
    )

    @Test
    fun `return empty list when risky venues are empty`() = runBlocking {
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
        assertEquals(1, matches[riskyVenues[0]]?.size)
        assertEquals(venueVisitRiskyInVenue1, matches[riskyVenues[0]]?.get(0))
    }

    @Test
    fun `returns no match when venue was visited outside of risky window`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf(venueVisitNotRiskyInVenue1)

        val matches = sut.findMatches(riskyVenues)

        assertEquals(0, matches.size)
    }

    @Test
    fun `returns no match when visit is in risky window but was previously flagged as risky`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf(venueVisitRiskyInVenue2AndWasInRiskyList)

        val matches = sut.findMatches(riskyVenues)

        assertEquals(0, matches.size)
    }

    @Test
    fun `returns matches when venue visit was in risky window`() = runBlocking {
        coEvery { visitedVenuesStorage.getVisits() } returns listOf(venueVisitRiskyInVenue2)

        val matches = sut.findMatches(riskyVenues)

        assertEquals(1, matches.size)
        assertEquals(1, matches[riskyVenues[1]]?.size)
        assertEquals(venueVisitRiskyInVenue2, matches[riskyVenues[1]]?.get(0))
    }

    @Test
    fun `return two matches if both venue visits are in risky window of venue`() = runBlocking {
        val venueVisits = listOf(venueVisitRiskyInVenue1, anotherVenueVisitRiskyInVenue1)

        coEvery { visitedVenuesStorage.getVisits() } returns venueVisits

        val matches = sut.findMatches(riskyVenues)

        assertEquals(1, matches.size)
        assertEquals(2, matches[riskyVenues[0]]?.size)
        assertEquals(venueVisitRiskyInVenue1, matches[riskyVenues[0]]?.get(0))
        assertEquals(anotherVenueVisitRiskyInVenue1, matches[riskyVenues[0]]?.get(1))
    }

    @Test
    fun `return matches in multiple risky venues`() = runBlocking {
        val venueVisits = listOf(venueVisitRiskyInVenue1, venueVisitRiskyInVenue2)

        coEvery { visitedVenuesStorage.getVisits() } returns venueVisits

        val matches = sut.findMatches(riskyVenues)

        assertEquals(2, matches.size)
        assertEquals(1, matches[riskyVenues[0]]?.size)
        assertEquals(1, matches[riskyVenues[1]]?.size)
        assertEquals(venueVisitRiskyInVenue1, matches[riskyVenues[0]]?.get(0))
        assertEquals(venueVisitRiskyInVenue2, matches[riskyVenues[1]]?.get(0))
    }

    @Test
    fun `intervals start equals others end`() = runBlocking {
        val interval1Start = Instant.parse("2020-08-24T00:00:00Z")
        val interval1 = Interval(
            start = interval1Start,
            inclusiveStart = true,
            end = Instant.parse("2020-08-26T00:00:00Z"),
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = interval1Start,
            inclusiveEnd = true
        )

        assertTrue { interval1.overlaps(interval2) }
        assertTrue { interval2.overlaps(interval1) }
    }

    @Test
    fun `intervals end equals others start`() = runBlocking {
        val interval1End = Instant.parse("2020-08-24T00:00:00Z")
        val interval1 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = interval1End,
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = interval1End,
            inclusiveStart = true,
            end = Instant.parse("2020-08-26T00:00:00Z"),
            inclusiveEnd = true
        )

        assertTrue { interval1.overlaps(interval2) }
        assertTrue { interval2.overlaps(interval1) }
    }

    @Test
    fun `interval intersection where interval2 in interval1`() = runBlocking {
        val interval1 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-28T00:00:00Z"),
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = Instant.parse("2020-08-23T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-26T00:00:00Z"),
            inclusiveEnd = true
        )

        assertTrue { interval1.overlaps(interval2) }
        assertTrue { interval2.overlaps(interval1) }
    }

    @Test
    fun `interval intersection where interval1 in interval2`() = runBlocking {
        val interval1 = Interval(
            start = Instant.parse("2020-08-23T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-28T00:00:00Z"),
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-28T00:00:00Z"),
            inclusiveEnd = true
        )

        assertTrue { interval1.overlaps(interval2) }
        assertTrue { interval2.overlaps(interval1) }
    }

    @Test
    fun `interval2 contains interval1`() = runBlocking {
        val interval1 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-23T00:00:00Z"),
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-28T00:00:00Z"),
            inclusiveEnd = true
        )

        assertTrue { interval1.overlaps(interval2) }
        assertTrue { interval2.overlaps(interval1) }
    }

    @Test
    fun `interval1 contains interval2`() = runBlocking {
        val interval1 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-28T00:00:00Z"),
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = Instant.parse("2020-08-23T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-28T00:00:00Z"),
            inclusiveEnd = true
        )

        assertTrue { interval1.overlaps(interval2) }
        assertTrue { interval2.overlaps(interval1) }
    }

    @Test
    fun `intervals have equal start and end`() = runBlocking {
        val interval1Start = Instant.parse("2020-08-22T00:00:00Z")
        val interval1End = Instant.parse("2020-08-24T00:00:00Z")
        val interval1 = Interval(
            start = interval1Start,
            inclusiveStart = true,
            end = interval1End,
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = interval1Start,
            inclusiveStart = true,
            end = interval1End,
            inclusiveEnd = true
        )

        assertTrue { interval1.overlaps(interval2) }
        assertTrue { interval2.overlaps(interval1) }
    }

    @Test
    fun `intervals intersect`() = runBlocking {
        val interval1 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-24T00:00:00Z"),
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = Instant.parse("2020-08-23T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-25T00:00:00Z"),
            inclusiveEnd = true
        )

        assertTrue { interval1.overlaps(interval2) }
        assertTrue { interval2.overlaps(interval1) }
    }

    @Test
    fun `intervals no not overlap`() = runBlocking {
        val interval1 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-24T00:00:00Z"),
            inclusiveEnd = true
        )
        val interval2 = Interval(
            start = Instant.parse("2020-08-25T00:00:00Z"),
            inclusiveStart = true,
            end = Instant.parse("2020-08-26T00:00:00Z"),
            inclusiveEnd = true
        )

        assertFalse { interval1.overlaps(interval2) }
        assertFalse { interval2.overlaps(interval1) }
    }

    @Test
    fun `intervals no not overlap with inclusive false`() = runBlocking {
        val interval1 = Interval(
            start = Instant.parse("2020-08-22T00:00:00Z"),
            inclusiveStart = false,
            end = Instant.parse("2020-08-24T00:00:00Z"),
            inclusiveEnd = false
        )
        val interval2 = Interval(
            start = Instant.parse("2020-08-25T00:00:00Z"),
            inclusiveStart = false,
            end = Instant.parse("2020-08-26T00:00:00Z"),
            inclusiveEnd = false
        )

        assertFalse { interval1.overlaps(interval2) }
        assertFalse { interval2.overlaps(interval1) }
    }
}
