package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateUtilsTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        every { context.resources.getString(R.string.locale) } returns "en"
    }

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

    @Test
    fun `daysUntilToday returns four days from now`() {
        val fromClock = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val toClock = Clock.fixed(Instant.parse("2014-12-25T10:23:00Z"), ZoneId.of("Europe/London"))

        val fromLocalDate = LocalDateTime.now(fromClock)

        val daysUntil = fromLocalDate.daysUntilToday(toClock)

        assertEquals(4, daysUntil)
    }

    @Test
    fun `hoursUntilNow returns four hours from now`() {
        val fromClock = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val toClock = Clock.fixed(Instant.parse("2014-12-21T10:23:00Z"), ZoneId.of("Europe/London"))

        val fromLocalDateTime = LocalDateTime.now(fromClock)

        val hoursUntil = fromLocalDateTime.hoursUntilNow(toClock)

        assertEquals(4, hoursUntil)
    }

    @Test
    fun `minutesUntilNow returns four minutes from now`() {
        val fromClock = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val toClock = Clock.fixed(Instant.parse("2014-12-21T06:27:00Z"), ZoneId.of("Europe/London"))

        val fromInstant = Instant.now(fromClock)

        val minutesUntil = fromInstant.minutesUntilNow(toClock)

        assertEquals(4, minutesUntil)
    }

    @Test
    fun `keysQueryFormat returns correct format`() {
        val clock = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val formatted = LocalDateTime.now(clock).keysQueryFormat()

        assertEquals("2014122106", formatted)
    }

    @Test
    fun `uiFormat for LocalDate returns correct format`() {
        val clock = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val formatted = LocalDate.now(clock).uiFormat(context)

        assertEquals("21 Dec 2014", formatted)
    }

    @Test
    fun `uiFormat for LocalDateTime returns correct format`() {
        val clock = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val formatted = LocalDateTime.now(clock).uiFormat(context)

        assertEquals("21 Dec 2014, 06:23", formatted)
    }

    @Test
    fun `uiFormat for LocalTime returns correct format`() {
        val clock = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val formatted = LocalTime.now(clock).uiFormat()

        assertEquals("06:23", formatted)
    }

    @Test
    fun `toISOSecondsFormat returns correct format`() {
        val clock = Clock.fixed(Instant.parse("2014-12-21T06:23:12.345Z"), ZoneId.of("Europe/London"))

        val formatted = Instant.now(clock).toISOSecondsFormat()

        assertEquals("2014-12-21T06:23:12Z", formatted)
    }

    @Test
    fun `isBeforeOrEqual for Instant returns true if before`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:22:59Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val instant1 = Instant.now(clock1)
        val instant2 = Instant.now(clock2)

        assertTrue(instant1.isBeforeOrEqual(instant2))
    }

    @Test
    fun `isBeforeOrEqual for Instant returns true if equal`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val instant1 = Instant.now(clock1)
        val instant2 = Instant.now(clock2)

        assertTrue(instant1.isBeforeOrEqual(instant2))
    }

    @Test
    fun `isBeforeOrEqual for Instant returns false if after`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:01Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val instant1 = Instant.now(clock1)
        val instant2 = Instant.now(clock2)

        assertFalse(instant1.isBeforeOrEqual(instant2))
    }

    @Test
    fun `isEqualOrAfter for Instant returns false if before`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:22:59Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val instant1 = Instant.now(clock1)
        val instant2 = Instant.now(clock2)

        assertFalse(instant1.isEqualOrAfter(instant2))
    }

    @Test
    fun `isEqualOrAfter for Instant returns true if equal`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val instant1 = Instant.now(clock1)
        val instant2 = Instant.now(clock2)

        assertTrue(instant1.isEqualOrAfter(instant2))
    }

    @Test
    fun `isEqualOrAfter for Instant returns true if after`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:01Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val instant1 = Instant.now(clock1)
        val instant2 = Instant.now(clock2)

        assertTrue(instant1.isEqualOrAfter(instant2))
    }

    @Test
    fun `isBeforeOrEqual for LocalDate returns true if before`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-20T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        assertTrue(localDate1.isBeforeOrEqual(localDate2))
    }

    @Test
    fun `isBeforeOrEqual for LocalDate returns true if equal`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        assertTrue(localDate1.isBeforeOrEqual(localDate2))
    }

    @Test
    fun `isBeforeOrEqual for LocalDate returns false if after`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-22T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        assertFalse(localDate1.isBeforeOrEqual(localDate2))
    }

    @Test
    fun `isEqualOrAfter for LocalDate returns false if before`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-20T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        assertFalse(localDate1.isEqualOrAfter(localDate2))
    }

    @Test
    fun `isEqualOrAfter for LocalDate returns true if equal`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        assertTrue(localDate1.isEqualOrAfter(localDate2))
    }

    @Test
    fun `isEqualOrAfter for LocalDate returns true if after`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-22T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        assertTrue(localDate1.isEqualOrAfter(localDate2))
    }

    @Test
    fun `isEqualOrAfter for LocalDateTime returns false if before`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-20T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDateTime.now(clock1)
        val localDate2 = LocalDateTime.now(clock2)

        assertFalse(localDate1.isEqualOrAfter(localDate2))
    }

    @Test
    fun `isEqualOrAfter for LocalDateTime returns true if equal`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDateTime.now(clock1)
        val localDate2 = LocalDateTime.now(clock2)

        assertTrue(localDate1.isEqualOrAfter(localDate2))
    }

    @Test
    fun `isEqualOrAfter for LocalDateTime returns true if after`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-22T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDateTime.now(clock1)
        val localDate2 = LocalDateTime.now(clock2)

        assertTrue(localDate1.isEqualOrAfter(localDate2))
    }

    @Test
    fun `selectEarliest returns first date if earlier`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-20T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        val result = selectEarliest(localDate1, localDate2)

        assertEquals(localDate1, result)
    }

    @Test
    fun `selectEarliest returns second date if earlier`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-20T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        val result = selectEarliest(localDate1, localDate2)

        assertEquals(localDate2, result)
    }

    @Test
    fun `selectEarliest returns any date if equal`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDate1 = LocalDate.now(clock1)
        val localDate2 = LocalDate.now(clock2)

        val result = selectEarliest(localDate1, localDate2)

        assertEquals(localDate1, result)
    }

    @Test
    fun `selectNewest returns first date-time if newer`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:22:59Z"), ZoneId.of("Europe/London"))

        val localDateTime1 = LocalDateTime.now(clock1)
        val localDateTime2 = LocalDateTime.now(clock2)

        val result = selectNewest(localDateTime1, localDateTime2)

        assertEquals(localDateTime1, result)
    }

    @Test
    fun `selectNewest returns second date-time if newer`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:22:59Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDateTime1 = LocalDateTime.now(clock1)
        val localDateTime2 = LocalDateTime.now(clock2)

        val result = selectNewest(localDateTime1, localDateTime2)

        assertEquals(localDateTime2, result)
    }

    @Test
    fun `selectNewest returns second date-time if first is null`() {
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:20:00Z"), ZoneId.of("Europe/London"))

        val localDateTime2 = LocalDateTime.now(clock2)

        val result = selectNewest(null, localDateTime2)

        assertEquals(localDateTime2, result)
    }

    @Test
    fun `selectNewest returns any date-time if equal`() {
        val clock1 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))
        val clock2 = Clock.fixed(Instant.parse("2014-12-21T06:23:00Z"), ZoneId.of("Europe/London"))

        val localDateTime1 = LocalDateTime.now(clock1)
        val localDateTime2 = LocalDateTime.now(clock2)

        val result = selectNewest(localDateTime1, localDateTime2)

        assertEquals(localDateTime1, result)
    }

    @Test
    fun `selectNewest LocalDate returns second date if newer`() {
        val localDate1 = LocalDate.of(2020, 7, 1)
        val localDate2 = LocalDate.of(2020, 7, 2)

        val result = selectNewest(localDate1, localDate2)

        assertEquals(localDate2, result)
    }

    @Test
    fun `selectNewest LocalDate returns first date if newer`() {
        val localDate1 = LocalDate.of(2020, 7, 3)
        val localDate2 = LocalDate.of(2020, 7, 2)

        val result = selectNewest(localDate1, localDate2)

        assertEquals(localDate1, result)
    }

    @Test
    fun `selectNewest LocalDate returns second date if first is null`() {
        val localDate2 = LocalDate.of(2020, 7, 2)

        val result = selectNewest(null, localDate2)

        assertEquals(localDate2, result)
    }

    @Test
    fun `selectNewest LocalDate returns any date if equal`() {
        val localDate1 = LocalDate.of(2020, 7, 3)
        val localDate2 = LocalDate.of(2020, 7, 3)

        val result = selectNewest(localDate1, localDate2)

        assertEquals(localDate1, result)
        assertEquals(localDate2, result)
    }
}
