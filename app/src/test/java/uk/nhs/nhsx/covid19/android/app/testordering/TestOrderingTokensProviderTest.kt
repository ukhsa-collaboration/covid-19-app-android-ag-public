package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingTokensProvider.Companion.TEST_ORDERING_TOKENS_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class TestOrderingTokensProviderTest : ProviderTest<TestOrderingTokensProvider, List<TestOrderPollingConfig>>() {

    override val getTestSubject = ::TestOrderingTokensProvider
    override val property = TestOrderingTokensProvider::configs
    override val key = TEST_ORDERING_TOKENS_KEY
    override val defaultValue: List<TestOrderPollingConfig> = emptyList()
    override val expectations: List<ProviderTestExpectation<List<TestOrderPollingConfig>>> = listOf(
        ProviderTestExpectation(
            json = POLLING_CONFIG_JSON,
            objectValue = listOf(POLLING_CONFIG),
            direction = JSON_TO_OBJECT
        )
    )

    @Test
    fun `add test ordering token to storage`() {
        sharedPreferencesReturns("")

        testSubject.add(POLLING_CONFIG)

        assertSharedPreferenceSetsValue(POLLING_CONFIG_JSON)
    }

    @Test
    fun `delete single test ordering token from storage`() {
        sharedPreferencesReturns(POLLING_CONFIG_JSON)

        testSubject.remove(POLLING_CONFIG)

        assertSharedPreferenceSetsValue("[]")
    }

    @Test
    fun `delete all test order polling tokens where condition resolves to true`() {
        sharedPreferencesReturns(MULTIPLE_POLLING_CONFIGS_JSON)

        val condition = mockk<TestOrderPollingConfig.() -> Boolean>()
        every { condition(POLLING_CONFIG) } returns false
        every { condition(POLLING_CONFIG1) } returns true
        every { condition(POLLING_CONFIG2) } returns true

        testSubject.removeAll { condition() }

        assertSharedPreferenceSetsValue(POLLING_CONFIG_JSON)
    }

    companion object {
        private val POLLING_CONFIG = TestOrderPollingConfig(
            Instant.ofEpochMilli(0),
            "pollingToken",
            "submissionToken"
        )
        private val POLLING_CONFIG1 = TestOrderPollingConfig(
            Instant.ofEpochMilli(Duration.of(1, ChronoUnit.HOURS).toMillis()),
            "pollingToken1",
            "submissionToken1"
        )
        private val POLLING_CONFIG2 = TestOrderPollingConfig(
            Instant.ofEpochMilli(Duration.of(2, ChronoUnit.HOURS).toMillis()),
            "pollingToken2",
            "submissionToken2"
        )

        private val POLLING_CONFIG_JSON =
            """
            [{"startedAt":"1970-01-01T00:00:00Z","testResultPollingToken":"pollingToken","diagnosisKeySubmissionToken":"submissionToken"}]
            """.trimIndent()

        private val MULTIPLE_POLLING_CONFIGS_JSON =
            """
            [{"startedAt":"1970-01-01T00:00:00Z","testResultPollingToken":"pollingToken","diagnosisKeySubmissionToken":"submissionToken"},{"startedAt":"1970-01-01T01:00:00Z","testResultPollingToken":"pollingToken1","diagnosisKeySubmissionToken":"submissionToken1"},{"startedAt":"1970-01-01T02:00:00Z","testResultPollingToken":"pollingToken2","diagnosisKeySubmissionToken":"submissionToken2"}]
            """.trimIndent()
    }
}
