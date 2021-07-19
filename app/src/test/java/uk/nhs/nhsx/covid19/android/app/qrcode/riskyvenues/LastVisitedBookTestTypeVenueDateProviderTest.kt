package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import io.mockk.every
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider.Companion.LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LastVisitedBookTestTypeVenueDateProviderTest : ProviderTest<LastVisitedBookTestTypeVenueDateProvider, LastVisitedBookTestTypeVenueDate?>() {
    private val fixedClock = Clock.fixed(Instant.parse("2020-12-01T10:00:00Z"), ZoneOffset.UTC)
    private val moshi = Moshi.Builder().add(LocalDateAdapter()).build()

    override val getTestSubject: (SharedPreferences) -> LastVisitedBookTestTypeVenueDateProvider = { sharedPreferences ->
        LastVisitedBookTestTypeVenueDateProvider(
            fixedClock,
            moshi,
            sharedPreferences
        )
    }
    override val property = LastVisitedBookTestTypeVenueDateProvider::lastVisitedVenue
    override val key = LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY
    override val defaultValue: LastVisitedBookTestTypeVenueDate? = null
    override val expectations: List<ProviderTestExpectation<LastVisitedBookTestTypeVenueDate?>> = listOf(
        ProviderTestExpectation(json = TEST_DATE_JSON, objectValue = TEST_DATE),
        ProviderTestExpectation(json = null, objectValue = null, direction = OBJECT_TO_JSON)
    )

    @Test
    fun `does not contain book test type risky venue when latest date is null`() {
        every { sharedPreferences.all[LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY] } returns null

        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            fixedClock,
            moshi,
            sharedPreferences
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertFalse(actual)
    }

    @Test
    fun `does not contain book test type risky venue when current date is before stored latest date`() {
        every { sharedPreferences.all[LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY] } returns TEST_DATE_JSON

        val fixedClock = Clock.fixed(Instant.parse("2020-11-30T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            fixedClock,
            moshi,
            sharedPreferences
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertFalse(actual)
    }

    @Test
    fun `contains book test type risky venue when latest date is equal to current date`() {
        every { sharedPreferences.all[LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY] } returns TEST_DATE_JSON

        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            fixedClock,
            moshi,
            sharedPreferences
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertTrue(actual)
    }

    @Test
    fun `contains book test type risky venue when current date is 10 days after stored latest date`() {
        every { sharedPreferences.all[LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY] } returns TEST_DATE_JSON

        val fixedClock = Clock.fixed(Instant.parse("2020-12-10T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            fixedClock,
            moshi,
            sharedPreferences
        )

        val actual = testSubject.containsBookTestTypeVenueAtRisk()

        assertTrue(actual)
    }

    @Test
    fun `does not contain book test type risky venue when current date is 11 days after stored latest date`() {
        every { sharedPreferences.all[LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY] } returns TEST_DATE_JSON

        val fixedClock = Clock.fixed(Instant.parse("2020-12-11T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = LastVisitedBookTestTypeVenueDateProvider(
            fixedClock,
            moshi,
            sharedPreferences
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
