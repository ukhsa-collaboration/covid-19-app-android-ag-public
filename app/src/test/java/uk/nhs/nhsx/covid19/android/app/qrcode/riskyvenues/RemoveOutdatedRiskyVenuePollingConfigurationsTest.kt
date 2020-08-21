package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class RemoveOutdatedRiskyVenuePollingConfigurationsTest {

    private val riskyVenuePollingConfigurationProvider =
        mockk<RiskyVenuePollingConfigurationProvider>(relaxed = true)
    private val isolationConfigurationProvider =
        mockk<IsolationConfigurationProvider>(relaxed = true)
    private var clock: Clock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)

    private val testSubject = RemoveOutdatedRiskyVenuePollingConfigurations(
        riskyVenuePollingConfigurationProvider,
        isolationConfigurationProvider,
        clock
    )

    private val durationDays = DurationDays(
        contactCase = 14,
        indexCaseSinceSelfDiagnosisOnset = 7,
        indexCaseSinceSelfDiagnosisUnknownOnset = 5,
        maxIsolation = 21,
        pendingTasksRetentionPeriod = 14
    )

    private val activePollingConfiguration = RiskyVenuePollingConfiguration(
        startedAt = Instant.now(clock).minus(13, ChronoUnit.DAYS),
        venueId = "1",
        approvalToken = "approval_token_1"
    )

    private val anotherActivePollingConfiguration = RiskyVenuePollingConfiguration(
        startedAt = Instant.now(clock).minus(14, ChronoUnit.DAYS),
        venueId = "2",
        approvalToken = "approval_token_2"
    )

    private val outdatedPollingConfiguration = RiskyVenuePollingConfiguration(
        startedAt = Instant.now(clock).minus(15, ChronoUnit.DAYS),
        venueId = "3",
        approvalToken = "approval_token_3"
    )

    private val anotherOutdatedPollingConfiguration = RiskyVenuePollingConfiguration(
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

    @Test
    fun `no outdated polling configs does not change polling configurations`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns activePollingConfigurations
        every { isolationConfigurationProvider.durationDays } returns durationDays

        testSubject.invoke()

        verify { isolationConfigurationProvider.durationDays }
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
        every { isolationConfigurationProvider.durationDays } returns durationDays

        testSubject.invoke()

        verify { isolationConfigurationProvider.durationDays }
        verify {
            riskyVenuePollingConfigurationProvider setProperty "configs" value eq(
                activePollingConfigurations
            )
        }
    }

    @Test
    fun `only outdated polling configs result in configurations provider empty`() = runBlocking {
        every { riskyVenuePollingConfigurationProvider.configs } returns outdatedPollingConfigurations
        every { isolationConfigurationProvider.durationDays } returns durationDays

        testSubject.invoke()

        verify { isolationConfigurationProvider.durationDays }
        verify {
            riskyVenuePollingConfigurationProvider setProperty "configs" value eq(
                listOf<RiskyVenuePollingConfiguration>()
            )
        }
    }
}
