package uk.nhs.nhsx.covid19.android.app.testordering

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Instant
import kotlin.test.assertEquals

class TestOrderingTokensProviderTest {

    private val testOrderingTokensStorage = mockk<TestOrderingTokensStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    @Test
    fun `add test ordering token`() {
        every { testOrderingTokensStorage.value } returns ""

        val testSubject = TestOrderingTokensProvider(testOrderingTokensStorage, moshi)
        testSubject.add(POLLING_CONFIG)

        verify { testOrderingTokensStorage.value = POLLING_CONFIG_JSON }
    }

    @Test
    fun `read empty test ordering token storage`() {
        every { testOrderingTokensStorage.value } returns null

        val testSubject = TestOrderingTokensProvider(testOrderingTokensStorage, moshi)
        val receivedConfig = testSubject.configs

        assertEquals(0, receivedConfig.size)
    }

    @Test
    fun `read test ordering token storage`() {
        every { testOrderingTokensStorage.value } returns POLLING_CONFIG_JSON

        val testSubject = TestOrderingTokensProvider(testOrderingTokensStorage, moshi)
        val receivedConfig = testSubject.configs

        assertEquals(1, receivedConfig.size)
        assertEquals(POLLING_CONFIG, receivedConfig[0])
    }

    @Test
    fun `read deleting ordering token storage`() {
        every { testOrderingTokensStorage.value } returns POLLING_CONFIG_JSON

        val testSubject = TestOrderingTokensProvider(testOrderingTokensStorage, moshi)
        testSubject.remove(POLLING_CONFIG)

        verify { testOrderingTokensStorage.value = "[]" }
    }

    companion object {
        val POLLING_CONFIG = TestOrderPollingConfig(
            Instant.ofEpochMilli(0),
            "pollingToken",
            "submissionToken"
        )

        val POLLING_CONFIG_JSON =
            """
            [{"startedAt":"1970-01-01T00:00:00Z","testResultPollingToken":"pollingToken","diagnosisKeySubmissionToken":"submissionToken"}]
            """.trimIndent()
    }
}
