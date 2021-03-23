package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Instant
import kotlin.test.assertEquals

class NextAnalyticsWindowToSubmitStorageTest {

    private val moshi = Moshi.Builder()
        .add(InstantAdapter())
        .build()

    private val nextAnalyticsWindowToSubmitJsonStorage = mockk<NextAnalyticsWindowToSubmitJsonStorage>(relaxUnitFun = true)
    private val analyticsLogStorage = mockk<AnalyticsLogStorage>()

    private val testSubject = NextAnalyticsWindowToSubmitStorage(nextAnalyticsWindowToSubmitJsonStorage, analyticsLogStorage, moshi)

    @Test
    fun `test serialize`() {
        testSubject.windowStartDate = instant

        verify {
            nextAnalyticsWindowToSubmitJsonStorage.value = jsonInstant
        }
    }

    @Test
    fun `returns null if no events and no window is stored`() {
        every { analyticsLogStorage.value } returns listOf()
        every { nextAnalyticsWindowToSubmitJsonStorage.value } returns null

        assertEquals(null, testSubject.windowStartDate)
    }

    @Test
    fun `returns value if window is stored`() {
        every { analyticsLogStorage.value } returns listOf()
        every { nextAnalyticsWindowToSubmitJsonStorage.value } returns jsonInstant

        assertEquals(instant, testSubject.windowStartDate)
    }

    @Test
    fun `returns value of oldest event if now window is stored`() {
        every { analyticsLogStorage.value } returns listOf(
            AnalyticsLogEntry(instant.plusSeconds(6), AnalyticsLogItem.Event(QR_CODE_CHECK_IN)),
            AnalyticsLogEntry(instant, AnalyticsLogItem.Event(QR_CODE_CHECK_IN)),
            AnalyticsLogEntry(instant.plusSeconds(30), AnalyticsLogItem.Event(QR_CODE_CHECK_IN)),
        )
        every { nextAnalyticsWindowToSubmitJsonStorage.value } returns null

        assertEquals(instant, testSubject.windowStartDate)
    }

    private val instant = Instant.parse("2020-10-10T08:00:00Z")
    private val jsonInstant =
        """
            "2020-10-10T08:00:00Z"
        """.trimIndent()
}
