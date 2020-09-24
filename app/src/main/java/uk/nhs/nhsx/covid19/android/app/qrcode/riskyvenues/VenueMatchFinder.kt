package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenue
import java.time.Instant
import javax.inject.Inject

typealias VenueId = String

class VenueMatchFinder @Inject constructor(private val visitedVenuesStorage: VisitedVenuesStorage) {

    suspend fun findMatches(
        riskyVenues: List<RiskyVenue>
    ): List<VenueId> = withContext(
        Dispatchers.IO
    ) {
        val venueVisits = visitedVenuesStorage.getVisits()
        val riskyVisitsToNotify = mutableSetOf<VenueId>()

        if (riskyVenues.isEmpty() || venueVisits.isEmpty()) return@withContext emptyList<VenueId>()

        venueVisits.forEach { venueVisit ->
            val riskyWindows = riskyVenues
                .filter { it.id == venueVisit.venue.id }
                .map { it.riskyWindow }

            val venueVisitInRiskyWindow = riskyWindows.any {
                overlaps(venueVisit.from, venueVisit.to, it.from, it.to)
            }

            if (!venueVisit.wasInRiskyList && venueVisitInRiskyWindow) {
                riskyVisitsToNotify.add(venueVisit.venue.id)
            }
        }

        return@withContext riskyVisitsToNotify.toList()
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
