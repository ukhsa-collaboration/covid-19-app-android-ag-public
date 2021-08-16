package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingTokensProvider.Companion.TEST_ORDERING_TOKENS_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Instant

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
    fun `add test ordering token`() {
        sharedPreferencesReturns("")

        testSubject.add(POLLING_CONFIG)

        assertSharedPreferenceSetsValue(POLLING_CONFIG_JSON)
    }

    @Test
    fun `read deleting ordering token storage`() {
        sharedPreferencesReturns(POLLING_CONFIG_JSON)

        testSubject.remove(POLLING_CONFIG)

        assertSharedPreferenceSetsValue("[]")
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
