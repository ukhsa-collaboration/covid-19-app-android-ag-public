package uk.nhs.nhsx.covid19.android.app.qrcode

import android.content.Context
import io.mockk.mockk
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals

class VenueVisitTest {

    private val context = mockk<Context>(relaxed = true)

    @Test
    fun `test venueVisit uiDate same day`() {

        val venueVisit = VenueVisit(
            venue = Venue("A", "Venue1"),
            from = Instant.parse("2020-07-25T10:00:00Z"),
            to = Instant.parse("2020-07-25T12:00:00Z")
        )

        assertEquals("25 Jul 2020 10:00 - 11:59", venueVisit.uiDate(context, ZoneOffset.UTC))
        assertEquals("25 Jul 2020 03:00 - 04:59", venueVisit.uiDate(context, ZoneId.of("America/Los_Angeles")))
    }

    @Test
    fun `test venueVisit uiDate end of day`() {

        val venueVisit = VenueVisit(
            venue = Venue("A", "Venue1"),
            from = Instant.parse("2020-07-25T10:00:00Z"),
            to = Instant.parse("2020-07-26T00:00:00Z")
        )

        assertEquals("25 Jul 2020 10:00 - 23:59", venueVisit.uiDate(context, ZoneOffset.UTC))
        assertEquals("25 Jul 2020 03:00 - 16:59", venueVisit.uiDate(context, ZoneId.of("America/Los_Angeles")))
    }

    @Test
    fun `test venueVisit uiDate 2 days`() {

        val venueVisit = VenueVisit(
            venue = Venue("A", "Venue1"),
            from = Instant.parse("2020-07-25T10:00:00Z"),
            to = Instant.parse("2020-07-26T08:00:00Z")
        )

        assertEquals("25 Jul 2020 10:00 - 07:59 26 Jul 2020", venueVisit.uiDate(context, ZoneOffset.UTC))
        assertEquals("25 Jul 2020 06:00 - 03:59 26 Jul 2020", venueVisit.uiDate(context, ZoneId.of("America/New_York")))
        assertEquals(
            "25 Jul 2020 03:00 - 00:59 26 Jul 2020",
            venueVisit.uiDate(context, ZoneId.of("America/Los_Angeles"))
        )
        assertEquals("25 Jul 2020 00:00 - 21:59", venueVisit.uiDate(context, ZoneId.of("US/Hawaii")))
    }
}
