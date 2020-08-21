package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

class RemoveOutdatedRiskyVenuePollingConfigurations(
    private val riskyVenuePollingConfigurationProvider: RiskyVenuePollingConfigurationProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val clock: Clock
) {

    @Inject
    constructor(
        riskyVenuePollingConfigurationProvider: RiskyVenuePollingConfigurationProvider,
        isolationConfigurationProvider: IsolationConfigurationProvider
    ) : this(
        riskyVenuePollingConfigurationProvider,
        isolationConfigurationProvider,
        clock = Clock.systemUTC()
    )

    fun invoke() {
        val maxDaysUntilExpiry =
            isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod

        val updatedRiskyVenuePollingConfigs =
            riskyVenuePollingConfigurationProvider.configs.filter {
                !Instant.now(clock).isAfter(it.startedAt.plus(maxDaysUntilExpiry.toLong(), DAYS))
            }

        riskyVenuePollingConfigurationProvider.configs = updatedRiskyVenuePollingConfigs
    }
}
