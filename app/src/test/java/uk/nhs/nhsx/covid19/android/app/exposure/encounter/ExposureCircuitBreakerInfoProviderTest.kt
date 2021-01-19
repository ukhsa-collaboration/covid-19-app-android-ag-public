package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class ExposureCircuitBreakerInfoProviderTest {

    private val storage = mockk<ExposureCircuitBreakerInfoStorage>(relaxed = true)
    private val moshi = Moshi.Builder().build()

    @Test
    fun `add exposure circuit breaker info item`() {
        every { storage.value } returns ITEM_WITHOUT_TOKEN

        val testSubject = ExposureCircuitBreakerInfoProvider(storage, moshi)

        testSubject.add(itemWithToken)

        verify { storage.value = MULTIPLE_ITEMS_JSON }
    }

    @Test
    fun `remove exposure circuit breaker info item`() {
        every { storage.value } returns MULTIPLE_ITEMS_JSON

        val testSubject = ExposureCircuitBreakerInfoProvider(storage, moshi)
        testSubject.remove(itemWithoutToken)

        verify { storage.value = ITEM_WITH_TOKEN }
    }

    @Test
    fun `set approval token for polling`() {
        every { storage.value } returns MULTIPLE_ITEMS_JSON

        val testSubject = ExposureCircuitBreakerInfoProvider(storage, moshi)
        testSubject.setApprovalToken(itemWithoutToken, "new_token")

        verify { storage.value = MULTIPLE_ITEMS_WITH_TOKEN_JSON }
    }

    @Test
    fun `read empty storage`() {
        every { storage.value } returns null

        val testSubject = ExposureCircuitBreakerInfoProvider(storage, moshi)
        val actual = testSubject.info

        assertEquals(listOf(), actual)
    }

    @Test
    fun `read corrupt storage`() {
        every { storage.value } returns "sdsfljghsfgyldfjg"

        val testSubject = ExposureCircuitBreakerInfoProvider(storage, moshi)
        val actual = testSubject.info

        assertEquals(listOf(), actual)
    }

    companion object {
        private val fixedClock = Clock.fixed(Instant.parse("2020-10-10T10:00:00Z"), ZoneOffset.UTC)
        private val startOfDayMillis =
            fixedClock.instant().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).toEpochMilli()

        private val itemWithoutToken = ExposureCircuitBreakerInfo(10.0, startOfDayMillis, 1, 2, fixedClock.millis())
        private val itemWithToken =
            ExposureCircuitBreakerInfo(13.0, startOfDayMillis, 1, 2, fixedClock.millis(), "token1")

        private val ITEM_WITHOUT_TOKEN =
            """
            [{"maximumRiskScore":10.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()}}]
            """.trim()

        private val ITEM_WITH_TOKEN =
            """
            [{"maximumRiskScore":13.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()},"approvalToken":"token1"}]
            """.trim()

        private val MULTIPLE_ITEMS_JSON =
            """
            [{"maximumRiskScore":10.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()}},{"maximumRiskScore":13.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()},"approvalToken":"token1"}]
            """.trim()

        private val MULTIPLE_ITEMS_WITH_TOKEN_JSON =
            """
            [{"maximumRiskScore":10.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()},"approvalToken":"new_token"},{"maximumRiskScore":13.0,"startOfDayMillis":$startOfDayMillis,"matchedKeyCount":1,"riskCalculationVersion":2,"exposureNotificationDate":${fixedClock.millis()},"approvalToken":"token1"}]
            """.trim()
    }
}
