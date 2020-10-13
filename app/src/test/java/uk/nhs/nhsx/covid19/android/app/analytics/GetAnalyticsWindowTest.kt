package uk.nhs.nhsx.covid19.android.app.analytics

import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class GetAnalyticsWindowTest {

    @Test
    fun `get window for current time start of day`() {

        val clock = Clock.fixed(Instant.parse("2020-07-28T00:05:00.00Z"), ZoneOffset.UTC)

        val testSubject = GetAnalyticsWindow(clock)

        val actual = testSubject.invoke()

        val expectedStart = Instant.parse("2020-07-28T00:00:00.00Z")
        val expectedEnd = Instant.parse("2020-07-29T00:00:00.00Z")

        assertEquals(expectedStart, actual.first)
        assertEquals(expectedEnd, actual.second)
    }
}
