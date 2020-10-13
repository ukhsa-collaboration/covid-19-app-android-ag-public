package uk.nhs.nhsx.covid19.android.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DateUtilsTest {

    @Test
    fun `roundDownToNearestQuarter returns rounded down to 0`() {
        val time = Instant.parse("2014-12-21T10:14:59Z")
        val roundedDown = Instant.parse("2014-12-21T10:00:00Z")

        val rounded = time.roundDownToNearestQuarter()

        assertEquals(roundedDown, rounded)
    }

    @Test
    fun `roundDownToNearestQuarter returns rounded down to 15`() {
        val time = Instant.parse("2014-12-21T10:29:59Z")
        val roundedDown = Instant.parse("2014-12-21T10:15:00Z")

        val rounded = time.roundDownToNearestQuarter()

        assertEquals(roundedDown, rounded)
    }

    @Test
    fun `roundDownToNearestQuarter returns rounded down to 30`() {
        val time = Instant.parse("2014-12-21T10:44:59Z")
        val roundedDown = Instant.parse("2014-12-21T10:30:00Z")

        val rounded = time.roundDownToNearestQuarter()

        assertEquals(roundedDown, rounded)
    }

    @Test
    fun `roundDownToNearestQuarter returns rounded down to 45`() {
        val time = Instant.parse("2014-12-21T10:59:59Z")
        val roundedDown = Instant.parse("2014-12-21T10:45:00Z")

        val rounded = time.roundDownToNearestQuarter()

        assertEquals(roundedDown, rounded)
    }

    @Test
    fun `roundUpToNearestQuarter returns rounded up to 15`() {
        val time = Instant.parse("2014-12-21T10:00:01Z")
        val roundedDown = Instant.parse("2014-12-21T10:15:00Z")

        val rounded = time.roundUpToNearestQuarter()

        assertEquals(roundedDown, rounded)
    }

    @Test
    fun `roundToNearestHour returns rounded up to 30`() {
        val time = Instant.parse("2014-12-21T10:15:01Z")
        val roundedDown = Instant.parse("2014-12-21T10:30:00Z")

        val rounded = time.roundUpToNearestQuarter()

        assertEquals(roundedDown, rounded)
    }

    @Test
    fun `roundToNearestHour returns rounded up to 45`() {
        val time = Instant.parse("2014-12-21T10:30:01Z")
        val roundedDown = Instant.parse("2014-12-21T10:45:00Z")

        val rounded = time.roundUpToNearestQuarter()

        assertEquals(roundedDown, rounded)
    }

    @Test
    fun `roundUpToNearestQuarter returns rounded up to next hour`() {
        val time = Instant.parse("2014-12-21T10:45:01Z")
        val roundedDown = Instant.parse("2014-12-21T11:00:00Z")

        val rounded = time.roundUpToNearestQuarter()

        assertEquals(roundedDown, rounded)
    }

    @Test
    fun getMidnightTimeReturnsNextDayMidnight() {
        val fromClock =
            Clock.fixed(Instant.parse("2014-12-21T10:23:00Z"), ZoneId.of("Europe/London"))

        val time = Instant.parse("2020-07-21T10:11:12Z")
        val localMidnightInUtc = Instant.parse("2020-07-21T23:00:00Z")

        val nextLocalMidnightTime = time.getNextLocalMidnightTime(fromClock)
        assertEquals(localMidnightInUtc, nextLocalMidnightTime)
    }
}
