package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.Venue
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class FilterOutdatedVisitsTest {

    private var clock: Clock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)

    private val testSubject = FilterOutdatedVisits(clock)

    @Test
    fun `filters dates 21 days old`() {

        val currentList = listOf(
            VenueVisit(
                venue = Venue(
                    "2",
                    organizationPartName = ""
                ),
                from = Instant.parse("2020-07-01T18:00:00.00Z"),
                to = Instant.parse("2020-07-04T01:00:00.00Z")
            )
        )

        val actual: List<VenueVisit> = testSubject.invoke(currentList)

        assertEquals(actual, emptyList<VenueVisit>())
    }

    @Test
    fun `return the same list if it doesn't include outdated visits`() {

        val currentList = listOf(
            VenueVisit(
                venue = Venue(
                    "2",
                    organizationPartName = ""
                ),
                from = Instant.parse("2020-07-09T18:00:00.00Z"),
                to = Instant.parse("2020-07-15T01:00:00.00Z")
            )
        )

        val actual: List<VenueVisit> = testSubject.invoke(currentList)

        assertEquals(actual, currentList)
    }

    @Test
    fun `return the same list if users hasn't checked out from a venue yet`() {

        val currentList = listOf(
            VenueVisit(
                venue = Venue(
                    "2",
                    organizationPartName = ""
                ),
                from = Instant.parse("2020-07-09T18:00:00.00Z"),
                to = Instant.parse("2020-07-10T00:00:00.00Z")
            )
        )

        val actual: List<VenueVisit> = testSubject.invoke(currentList)

        assertEquals(actual, currentList)
    }
}
