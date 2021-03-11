package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LastVisitedBookTestTypeVenueDateProviderTest {

    private val lastVisitedBookTestTypeVenueDateStorage = mockk<LastVisitedBookTestTypeVenueDateStorage>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-12-01T10:00:00Z"), ZoneOffset.UTC)
    private val moshi = Moshi.Builder().add(LocalDateAdapter()).build()

    @Test
    fun `fetching latest stored date when it was not set before should return null`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns null

        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        val actual = testSubject.lastVisitedVenue

        assertNull(actual)
    }

    @Test
    fun `fetching latest stored date when value is stored in shared preferences should return correct value`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns TEST_DATE_JSON

        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        val actual = testSubject.lastVisitedVenue

        assertEquals(TEST_DATE, actual)
    }

    @Test
    fun `save last stored date`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns null

        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        testSubject.lastVisitedVenue = TEST_DATE

        verify { lastVisitedBookTestTypeVenueDateStorage.value = TEST_DATE_JSON }
    }

    @Test
    fun `clear last stored date`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns TEST_DATE_JSON

        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        testSubject.lastVisitedVenue = null

        verify { lastVisitedBookTestTypeVenueDateStorage.value = null }
    }

    @Test
    fun `does not contain book test type risky venue when latest date is null`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns null

        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertFalse(actual)
    }

    @Test
    fun `does not contain book test type risky venue when current date is before stored latest date`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns TEST_DATE_JSON

        val fixedClock = Clock.fixed(Instant.parse("2020-11-30T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertFalse(actual)
    }

    @Test
    fun `contains book test type risky venue when latest date is equal to current date`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns TEST_DATE_JSON

        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertTrue(actual)
    }

    @Test
    fun `contains book test type risky venue when current date is 10 days after stored latest date`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns TEST_DATE_JSON

        val fixedClock = Clock.fixed(Instant.parse("2020-12-10T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertTrue(actual)
    }

    @Test
    fun `does not contain book test type risky venue when current date is 11 days after stored latest date`() {
        every { lastVisitedBookTestTypeVenueDateStorage.value } returns TEST_DATE_JSON

        val fixedClock = Clock.fixed(Instant.parse("2020-12-11T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            lastVisitedBookTestTypeVenueDateStorage,
            fixedClock,
            moshi
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertFalse(actual)
    }

    companion object {
        private val TEST_DATE_JSON =
            """
            {"latestDate":"2020-12-01","riskyVenueConfigurationDurationDays":{"optionToBookATest":10}}
            """.trimIndent()

        private val TEST_DATE = LastVisitedBookTestTypeVenueDate(
            LocalDate.parse("2020-12-01"),
            RiskyVenueConfigurationDurationDays(optionToBookATest = 10)
        )
    }
}
