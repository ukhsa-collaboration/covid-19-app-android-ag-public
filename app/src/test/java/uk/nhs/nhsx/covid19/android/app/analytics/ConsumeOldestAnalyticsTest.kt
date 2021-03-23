package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.ConsumeOldestAnalyticsResult.Consumed
import uk.nhs.nhsx.covid19.android.app.analytics.ConsumeOldestAnalyticsResult.NothingToConsume
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class ConsumeOldestAnalyticsTest {

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxUnitFun = true)
    private val nextAnalyticsWindowToSubmitStorage = mockk<NextAnalyticsWindowToSubmitStorage>(relaxUnitFun = true)
    private val now = Instant.parse("2020-09-29T00:05:00.00Z")
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)

    val testSubject = ConsumeOldestAnalytics(
        analyticsLogStorage,
        GetAnalyticsWindow(fixedClock),
        nextAnalyticsWindowToSubmitStorage,
        fixedClock
    )

    @Test
    fun `returns group with entries if available`() = runBlocking {
        every { nextAnalyticsWindowToSubmitStorage.windowStartDate } returns now.minus(5, ChronoUnit.DAYS)
        every { analyticsLogStorage.value } returns logEntries

        val result = testSubject.invoke()

        val expectedWindowStart = "2020-09-24T00:00:00Z"
        val expectedWindowEnd = "2020-09-25T00:00:00Z"
        verify {
            analyticsLogStorage.remove(
                startInclusive = Instant.parse(expectedWindowStart),
                endExclusive = Instant.parse(expectedWindowEnd)
            )
        }
        verify { nextAnalyticsWindowToSubmitStorage.windowStartDate = Instant.parse(expectedWindowEnd) }

        val expected = Consumed(
            AnalyticsEventsGroup(
                analyticsWindow = AnalyticsWindow(
                    startDate = expectedWindowStart,
                    endDate = expectedWindowEnd
                ),
                entries = listOf(
                    AnalyticsLogEntry(now.minus(5, ChronoUnit.DAYS), Event(QR_CODE_CHECK_IN)),
                    AnalyticsLogEntry(now.minus(5, ChronoUnit.DAYS), Event(QR_CODE_CHECK_IN))
                )
            )
        )

        assertEquals(expected, result)
    }

    @Test
    fun `returns group with no entries`() = runBlocking {
        every { nextAnalyticsWindowToSubmitStorage.windowStartDate } returns now.minus(6, ChronoUnit.DAYS)
        every { analyticsLogStorage.value } returns logEntries

        val result = testSubject.invoke()

        val expectedWindowStart = "2020-09-23T00:00:00Z"
        val expectedWindowEnd = "2020-09-24T00:00:00Z"
        verify {
            analyticsLogStorage.remove(
                startInclusive = Instant.parse(expectedWindowStart),
                endExclusive = Instant.parse(expectedWindowEnd)
            )
        }
        verify { nextAnalyticsWindowToSubmitStorage.windowStartDate = Instant.parse(expectedWindowEnd) }

        val expected = Consumed(
            AnalyticsEventsGroup(
                analyticsWindow = AnalyticsWindow(
                    startDate = expectedWindowStart,
                    endDate = expectedWindowEnd
                ),
                entries = listOf()
            )
        )

        assertEquals(expected, result)
    }

    @Test
    fun `test returns null if today is reached`() = runBlocking {
        every { nextAnalyticsWindowToSubmitStorage.windowStartDate } returns now.minusSeconds(5)
        every { analyticsLogStorage.value } returns emptyList()

        val result = testSubject.invoke()

        verify(exactly = 0) { analyticsLogStorage.remove(any(), any()) }
        verify(exactly = 0) { nextAnalyticsWindowToSubmitStorage.windowStartDate = any() }

        assertEquals(NothingToConsume, result)
    }

    @Test
    fun `test returns empty list when no entries available for one day`() = runBlocking {
        every { nextAnalyticsWindowToSubmitStorage.windowStartDate } returns null
        every { analyticsLogStorage.value } returns emptyList()

        val result = testSubject.invoke()

        verify(exactly = 0) { analyticsLogStorage.remove(any(), any()) }
        verify(exactly = 0) { nextAnalyticsWindowToSubmitStorage.windowStartDate = any() }

        assertEquals(NothingToConsume, result)
    }

    private val logEntries = listOf(
        AnalyticsLogEntry(now.minus(5, ChronoUnit.DAYS), Event(QR_CODE_CHECK_IN)),
        AnalyticsLogEntry(now.minus(5, ChronoUnit.DAYS), Event(QR_CODE_CHECK_IN)),
        AnalyticsLogEntry(now.minus(3, ChronoUnit.DAYS), Event(QR_CODE_CHECK_IN)),
        AnalyticsLogEntry(now.minus(2, ChronoUnit.DAYS), Event(QR_CODE_CHECK_IN)),
        AnalyticsLogEntry(now.minus(1, ChronoUnit.DAYS), Event(QR_CODE_CHECK_IN)),
    )
}
