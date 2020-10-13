package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ExposureNotificationTokensProviderTest {

    private val storage = mockk<ExposureNotificationTokensStorage>(relaxed = true)
    private val moshi = Moshi.Builder().build()

    @Test
    fun `add token`() {
        every { storage.value } returns SINGLE_TOKEN_JSON

        val testSubject = ExposureNotificationTokensProvider(storage, moshi, fixedClock)
        testSubject.add("token2")

        verify { storage.value = MULTIPLE_TOKENS_JSON }
    }

    @Test
    fun `remove token`() {
        every { storage.value } returns MULTIPLE_TOKENS_JSON

        val testSubject = ExposureNotificationTokensProvider(storage, moshi, fixedClock)
        testSubject.remove("token2")

        verify { storage.value = SINGLE_TOKEN_JSON }
    }

    @Test
    fun `update polling token`() {
        every { storage.value } returns MULTIPLE_TOKENS_JSON

        val testSubject = ExposureNotificationTokensProvider(storage, moshi, fixedClock)
        testSubject.updateToPolling("token2", 123)

        verify { storage.value = MULTIPLE_POLLING_TOKENS_JSON }
    }

    @Test
    fun `read empty storage`() {
        every { storage.value } returns null

        val testSubject = ExposureNotificationTokensProvider(storage, moshi, fixedClock)
        val actual = testSubject.tokens

        assertEquals(listOf(), actual)
    }

    @Test
    fun `read corrupt storage`() {
        every { storage.value } returns "sdsfljghsfgyldfjg"

        val testSubject = ExposureNotificationTokensProvider(storage, moshi, fixedClock)
        val actual = testSubject.tokens

        assertEquals(listOf(), actual)
    }

    companion object {
        private val fixedClock = Clock.fixed(Instant.parse("2020-10-10T10:00:00Z"), ZoneOffset.UTC)
        private val SINGLE_TOKEN_JSON =
            """
            [{"token":"token1","exposureDate":123,"startedAt":${fixedClock.millis()}}]
            """.trim()

        private val MULTIPLE_TOKENS_JSON =
            """
            [{"token":"token1","exposureDate":123,"startedAt":${fixedClock.millis()}},{"token":"token2","startedAt":${fixedClock.millis()}}]
            """.trim()

        private val MULTIPLE_POLLING_TOKENS_JSON =
            """
            [{"token":"token1","exposureDate":123,"startedAt":${fixedClock.millis()}},{"token":"token2","exposureDate":123,"startedAt":${fixedClock.millis()}}]
            """.trim()
    }
}
