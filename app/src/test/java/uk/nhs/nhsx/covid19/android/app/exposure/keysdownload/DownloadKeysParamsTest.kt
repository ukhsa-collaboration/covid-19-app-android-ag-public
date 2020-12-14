package uk.nhs.nhsx.covid19.android.app.exposure.keysdownload

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Daily
import uk.nhs.nhsx.covid19.android.app.exposure.keysdownload.DownloadKeysParams.Intervals.Hourly
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals

class DownloadKeysParamsTest {

    private var clock: Clock = Clock.fixed(Instant.parse("2014-12-21T10:00:00Z"), ZoneOffset.UTC)

    private val lastDownloadedKeyProvider = mockk<LastDownloadedKeyTimeProvider>(relaxed = true)

    private val testSubject = DownloadKeysParams(lastDownloadedKeyProvider, clock)

    @Test
    fun `last keys downloaded less than 2 hours ago`() {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.now(clock)
            .minusHours(1)

        val nextQueries = testSubject.getNextQueries()

        assertEquals(listOf(), nextQueries)
    }

    @Test
    fun `last keys downloaded three months ago should return query parameters for keys of last two weeks`() {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.now(clock).minusMonths(3)

        val nextQueries = testSubject.getNextQueries()

        val expectedDateTimeList = listOf(
            Daily("2014120800"),
            Daily("2014120900"),
            Daily("2014121000"),
            Daily("2014121100"),
            Daily("2014121200"),
            Daily("2014121300"),
            Daily("2014121400"),
            Daily("2014121500"),
            Daily("2014121600"),
            Daily("2014121700"),
            Daily("2014121800"),
            Daily("2014121900"),
            Daily("2014122000"),
            Daily("2014122100"),
            Hourly("2014122102"),
            Hourly("2014122104"),
            Hourly("2014122106"),
            Hourly("2014122108"),
            Hourly("2014122110")
        )

        assertEquals(expectedDateTimeList, nextQueries)
    }

    @Test
    fun `will do one two-hourly incremental download`() {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.of(
            2014,
            12,
            21,
            8,
            0
        )

        val nextQueries = testSubject.getNextQueries()

        val expectedDateTime: List<Intervals> = listOf(Hourly("2014122110"))

        assertEquals(expectedDateTime, nextQueries)
    }

    @Test
    fun `will do two two-hourly incremental downloads`() {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.of(
            2014,
            12,
            21,
            6,
            0
        )

        val nextQueries = testSubject.getNextQueries()

        val expectedDateTimeList = listOf(
            Hourly("2014122108"),
            Hourly("2014122110")
        )

        assertEquals(expectedDateTimeList, nextQueries)
    }

    @Test
    fun `test one daily with one hourly before daily and hourly in current day incremental downloads `() {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.of(
            2014,
            12,
            19,
            22,
            0
        )

        val nextQueries = testSubject.getNextQueries()

        val expectedDateTimeList: List<Intervals> = listOf(
            Hourly("2014122000"),
            Daily("2014122100"),
            Hourly("2014122102"),
            Hourly("2014122104"),
            Hourly("2014122106"),
            Hourly("2014122108"),
            Hourly("2014122110")
        )

        assertEquals(expectedDateTimeList, nextQueries)
    }

    @Test
    fun `test one daily with two hourly before daily and hourly in current day incremental downloads `() {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.of(
            2014,
            12,
            19,
            20,
            0
        )

        val nextQueries = testSubject.getNextQueries()

        val expectedDateTimeList = listOf(
            Hourly("2014121922"),
            Hourly("2014122000"),
            Daily("2014122100"),
            Hourly("2014122102"),
            Hourly("2014122104"),
            Hourly("2014122106"),
            Hourly("2014122108"),
            Hourly("2014122110")
        )

        assertEquals(expectedDateTimeList, nextQueries)
    }

    @Test
    fun `test three daily with two hourly before daily and hourly in current day incremental downloads `() {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.of(
            2014,
            12,
            17,
            20,
            0
        )

        // not 21 day // hour 10

        val nextQueries = testSubject.getNextQueries()

        val expectedDateTimeList: List<Intervals> = listOf(
            Hourly("2014121722"),
            Hourly("2014121800"),
            Daily("2014121900"),
            Daily("2014122000"),
            Daily("2014122100"),
            Hourly("2014122102"),
            Hourly("2014122104"),
            Hourly("2014122106"),
            Hourly("2014122108"),
            Hourly("2014122110")
        )

        assertEquals(expectedDateTimeList, nextQueries)
    }

    @Test
    fun `test last update was at midnight`() {

        every { lastDownloadedKeyProvider.getLatestStoredTime() } returns LocalDateTime.of(
            2014,
            12,
            17,
            0,
            0
        )

        val nextQueries = testSubject.getNextQueries()

        val expectedDateTimeList: List<Intervals> = listOf(
            Daily("2014121800"),
            Daily("2014121900"),
            Daily("2014122000"),
            Daily("2014122100"),
            Hourly("2014122102"),
            Hourly("2014122104"),
            Hourly("2014122106"),
            Hourly("2014122108"),
            Hourly("2014122110")
        )

        assertEquals(expectedDateTimeList, nextQueries)
    }
}
