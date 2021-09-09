package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderPollingConfig
import uk.nhs.nhsx.covid19.android.app.testordering.TestOrderingTokensProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClearOutdatedTestOrderPollingConfigsTest {

    private val testOrderingTokensProvider = mockk<TestOrderingTokensProvider>(relaxUnitFun = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val fixedClock = Clock.fixed(Instant.parse("2021-01-30T10:00:00.00Z"), ZoneOffset.UTC)

    private val clearOutdatedTestOrderPollingConfigs = ClearOutdatedTestOrderPollingConfigs(
        testOrderingTokensProvider,
        isolationConfigurationProvider,
        fixedClock
    )

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns DurationDays(
            testResultPollingTokenRetentionPeriod = 28
        )
    }

    @Test
    fun `only remove outdated test result polling tokens`() {
        every { testOrderingTokensProvider.configs } returns
                listOf(outdatedConfig1, outdatedConfig2, pollingConfig1, pollingConfig2)

        val conditionSlot = slot<(TestOrderPollingConfig.() -> Boolean)>()

        clearOutdatedTestOrderPollingConfigs()

        verify { testOrderingTokensProvider.removeAll(capture(conditionSlot)) }

        with(conditionSlot.captured) {
            assertTrue(this(outdatedConfig1))
            assertTrue(this(outdatedConfig2))
            assertFalse(this(pollingConfig1))
            assertFalse(this(pollingConfig2))
        }
    }

    // Token created 28 days and 1 hour ago. This token is outdated and should be deleted.
    private val outdatedConfig1 = TestOrderPollingConfig(
        startedAt = Instant.parse("2021-01-02T09:00:00.00Z"),
        testResultPollingToken = "pollingToken1",
        diagnosisKeySubmissionToken = "submissionToken1"
    )

    /**
     * Token created 27 days and 22 hours ago. Since we ignore the time portion (truncate to days) this token is
     * considered outdated (28 days old) and should be deleted.
     */
    private val outdatedConfig2 = TestOrderPollingConfig(
        startedAt = Instant.parse("2021-01-02T12:00:00.00Z"),
        testResultPollingToken = "pollingToken1",
        diagnosisKeySubmissionToken = "submissionToken1"
    )

    // Token created 27 days and 1 hour ago. This token is not outdated.
    private val pollingConfig1 = TestOrderPollingConfig(
        startedAt = Instant.parse("2021-01-03T09:00:00.00Z"),
        testResultPollingToken = "pollingToken2",
        diagnosisKeySubmissionToken = "submissionToken2"
    )

    // Token created 26 days and 23 hour ago. This token is not outdated.
    private val pollingConfig2 = TestOrderPollingConfig(
        startedAt = Instant.parse("2021-01-03T11:00:00.00Z"),
        testResultPollingToken = "pollingToken3",
        diagnosisKeySubmissionToken = "submissionToken3"
    )
}
