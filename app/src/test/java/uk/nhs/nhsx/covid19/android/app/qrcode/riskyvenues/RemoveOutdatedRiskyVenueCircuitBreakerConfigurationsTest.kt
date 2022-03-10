package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class RemoveOutdatedRiskyVenueCircuitBreakerConfigurationsTest {

    private val riskyVenuePollingConfigurationProvider =
        mockk<RiskyVenueCircuitBreakerConfigurationProvider>(relaxed = true)
    private val getLatestConfiguration =
        mockk<GetLatestConfiguration>(relaxed = true)
    private var clock: Clock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)
    private val configuration = mockk<CountrySpecificConfiguration>()

    private val testSubject = RemoveOutdatedRiskyVenuePollingConfigurations(
        riskyVenuePollingConfigurationProvider,
        getLatestConfiguration,
        clock
    )

    private val activePollingConfiguration = RiskyVenueCircuitBreakerConfiguration(
        startedAt = Instant.now(clock).minus(13, ChronoUnit.DAYS),
        venueId = "1",
        approvalToken = "approval_token_1"
    )

    private val anotherActivePollingConfiguration = RiskyVenueCircuitBreakerConfiguration(
        startedAt = Instant.now(clock).minus(14, ChronoUnit.DAYS),
        venueId = "2",
        approvalToken = "approval_token_2"
    )

    private val outdatedPollingConfiguration = RiskyVenueCircuitBreakerConfiguration(
        startedAt = Instant.now(clock).minus(15, ChronoUnit.DAYS),
        venueId = "3",
        approvalToken = "approval_token_3"
    )

    private val anotherOutdatedPollingConfiguration = RiskyVenueCircuitBreakerConfiguration(
        startedAt = Instant.now(clock).minus(20, ChronoUnit.DAYS),
        venueId = "4",
        approvalToken = "approval_token_4"
    )

    private val activePollingConfigurations = listOf(
        activePollingConfiguration,
        anotherActivePollingConfiguration
    )

    private val outdatedPollingConfigurations = listOf(
        outdatedPollingConfiguration,
        anotherOutdatedPollingConfiguration
    )

    @Before
    fun setUp() {
        every { getLatestConfiguration() } returns configuration
        every { configuration.pendingTasksRetentionPeriod } returns 14
    }

    @Test
    fun `no outdated polling configs does not change polling configurations`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns activePollingConfigurations

        testSubject.invoke()

        verify { configuration.pendingTasksRetentionPeriod }
        verify {
            riskyVenuePollingConfigurationProvider setProperty "configs" value eq(
                activePollingConfigurations
            )
        }
    }

    @Test
    fun `outdated polling configs removed from polling configurations provider`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns listOf(
            activePollingConfigurations,
            outdatedPollingConfigurations
        ).flatten()

        testSubject.invoke()

        verify { configuration.pendingTasksRetentionPeriod }
        verify {
            riskyVenuePollingConfigurationProvider setProperty "configs" value eq(
                activePollingConfigurations
            )
        }
    }

    @Test
    fun `only outdated polling configs result in configurations provider empty`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns outdatedPollingConfigurations

        testSubject.invoke()

        verify { configuration.pendingTasksRetentionPeriod }
        verify {
            riskyVenuePollingConfigurationProvider setProperty "configs" value eq(
                listOf<RiskyVenueCircuitBreakerConfiguration>()
            )
        }
    }
}
