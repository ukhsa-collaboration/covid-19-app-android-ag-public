package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import java.time.Instant

class GetOldestLogEntryInstantTest {
    private val instant = Instant.parse("2020-10-10T08:00:00Z")

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>()

    private val testSubject = GetOldestLogEntryInstant(analyticsLogStorage)

    @Test
    fun `returns null if nothing is stored`() {
        every { analyticsLogStorage.value } returns listOf()

        assertNull(testSubject.invoke())
    }

    @Test
    fun `returns value of oldest event when multiple are stored`() {
        every { analyticsLogStorage.value } returns listOf(
            AnalyticsLogEntry(instant.plusSeconds(6), AnalyticsLogItem.Event(QR_CODE_CHECK_IN)),
            AnalyticsLogEntry(instant, AnalyticsLogItem.Event(QR_CODE_CHECK_IN)),
            AnalyticsLogEntry(instant.plusSeconds(30), AnalyticsLogItem.Event(QR_CODE_CHECK_IN)),
        )

        assertEquals(instant, testSubject.invoke())
    }
}
