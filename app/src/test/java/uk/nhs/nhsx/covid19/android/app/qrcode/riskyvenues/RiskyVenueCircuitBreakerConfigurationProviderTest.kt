package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.MessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.RiskyVenueMessageTypeAdapter
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class RiskyVenueCircuitBreakerConfigurationProviderTest {

    private val storage = mockk<RiskyVenuePollingConfigurationJsonStorage>(relaxed = true)
    private val moshi = Moshi.Builder()
        .add(InstantAdapter())
        .add(RiskyVenueMessageTypeAdapter())
        .build()

    @Test
    fun `test adding polling config`() {
        every { storage.value } returns SINGLE_POLLING_CONFIGURATION_JSON

        val testSubject = RiskyVenueCircuitBreakerConfigurationProvider(storage, moshi)
        testSubject.add(pollingConfig)

        verify { storage.value = MULTIPLE_POLLING_CONFIGURATION_JSON }
    }

    @Test
    fun `test removing polling config`() {
        every { storage.value } returns MULTIPLE_POLLING_CONFIGURATION_JSON

        val testSubject = RiskyVenueCircuitBreakerConfigurationProvider(storage, moshi)
        testSubject.remove(pollingConfig)

        verify { storage.value = SINGLE_POLLING_CONFIGURATION_JSON }
    }

    @Test
    fun `test with empty storage`() {
        every { storage.value } returns null

        val testSubject = RiskyVenueCircuitBreakerConfigurationProvider(storage, moshi)
        val actual = testSubject.configs

        assertEquals(listOf(), actual)
    }

    @Test
    fun `test with corrupt storage`() {
        every { storage.value } returns "sdsfljghsfgyldfjg"

        val testSubject = RiskyVenueCircuitBreakerConfigurationProvider(storage, moshi)
        val actual = testSubject.configs

        assertEquals(listOf(), actual)
    }

    companion object {
        private val fixedClock =
            Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)

        private val pollingConfig = RiskyVenueCircuitBreakerConfiguration(
            startedAt = Instant.now(fixedClock),
            venueId = "venue2",
            approvalToken = "token2",
            messageType = BOOK_TEST
        )

        private val SINGLE_POLLING_CONFIGURATION_JSON =
            """
            [{"startedAt":"2020-07-27T01:00:00Z","venueId":"venue1","approvalToken":"token1","isPolling":true,"messageType":"M1"}]
            """.trim()

        private val MULTIPLE_POLLING_CONFIGURATION_JSON =
            """
            [{"startedAt":"2020-07-27T01:00:00Z","venueId":"venue1","approvalToken":"token1","isPolling":true,"messageType":"M1"},{"startedAt":"2020-07-28T01:00:00Z","venueId":"venue2","approvalToken":"token2","isPolling":true,"messageType":"M2"}]
            """.trim()
    }
}
