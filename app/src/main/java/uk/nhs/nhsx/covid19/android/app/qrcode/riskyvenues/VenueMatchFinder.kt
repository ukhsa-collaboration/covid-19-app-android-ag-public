package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import java.time.Instant
import javax.inject.Inject

typealias VenueId = String

class VenueMatchFinder @Inject constructor(private val visitedVenuesStorage: VisitedVenuesStorage) {

    suspend fun findMatches(
        riskyVenues: List<RiskyVenue>
    ): Map<RiskyVenue, List<VenueVisit>> = withContext(
        Dispatchers.IO
    ) {
        val venueVisits = visitedVenuesStorage.getVisits()
        val riskyVisitsToNotify = mutableMapOf<RiskyVenue, List<VenueVisit>>()

        if (riskyVenues.isEmpty() || venueVisits.isEmpty()) return@withContext emptyMap<RiskyVenue, List<VenueVisit>>()

        riskyVenues.forEach { riskyVenue ->
            val unnotifiedVisitsInRiskyWindow = venueVisits.filter { venueVisit ->
                !venueVisit.wasInRiskyList &&
                    venueVisit.venue.id == riskyVenue.id &&
                    overlaps(venueVisit.from, venueVisit.to, riskyVenue.riskyWindow.from, riskyVenue.riskyWindow.to)
            }
            if (unnotifiedVisitsInRiskyWindow.isNotEmpty()) {
                riskyVisitsToNotify[riskyVenue] = unnotifiedVisitsInRiskyWindow
            }
        }

        return@withContext riskyVisitsToNotify
    }

    private fun overlaps(
        visitedVenueFrom: Instant,
        visitedVenueTo: Instant,
        riskyVenueFrom: Instant,
        riskyVenueTo: Instant
    ): Boolean {
        val visitInterval = Interval(
            start = visitedVenueFrom,
            inclusiveStart = true,
            end = visitedVenueTo,
            inclusiveEnd = false
        )
        val riskyInterval = Interval(
            start = riskyVenueFrom,
            inclusiveStart = true,
            end = riskyVenueTo,
            inclusiveEnd = false
        )
        return visitInterval.overlaps(riskyInterval)
    }

    class Interval(
        private val start: Instant,
        private val inclusiveStart: Boolean,
        private val end: Instant,
        private val inclusiveEnd: Boolean
    ) {

        fun overlaps(other: Interval): Boolean {

            // intervals share at least one point in time
            if ((start == other.end && inclusiveStart && other.inclusiveEnd) ||
                (end == other.start && inclusiveEnd && other.inclusiveStart)
            ) {
                return true
            }

            // intervals intersect
            if (end.isAfter(other.start) && start.isBefore(other.start) ||
                other.end.isAfter(start) && other.start.isBefore(start)
            ) {
                return true
            }

            // this interval contains the other interval
            if ((start == other.start && other.inclusiveStart || start.isAfter(other.start)) &&
                (end == other.end && other.inclusiveStart || end.isBefore(other.end))
            ) {
                return true
            }

            // the other interval contains this interval
            return (
                (other.start == start && inclusiveStart || other.start.isAfter(start)) &&
                    (other.end == end && inclusiveEnd || other.end.isBefore(end))
                )
        }
    }
}
