package uk.nhs.nhsx.covid19.android.app.common

import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenueConfigurationApi
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ClearOutdatedDataAndUpdateIsolationConfiguration @Inject constructor(
    private val resetIsolationStateIfNeeded: ResetIsolationStateIfNeeded,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val isolationConfigurationApi: IsolationConfigurationApi,
    private val riskyVenueConfigurationProvider: RiskyVenueConfigurationProvider,
    private val riskyVenueConfigurationApi: RiskyVenueConfigurationApi,
    @Suppress("DEPRECATION")
    private val exposureNotificationTokensProvider: ExposureNotificationTokensProvider,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val clearOutdatedKeySharingInfo: ClearOutdatedKeySharingInfo,
    private val clock: Clock
) {

    suspend operator fun invoke(): Result<Unit> = runSafely {
        updateIsolationConfiguration()
        updateRiskyVenueConfiguration()

        resetIsolationStateIfNeeded()

        if (!lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk()) {
            lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = null
        }

        clearOutdatedKeySharingInfo()

        val retentionPeriodDays = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        clearOldEpidemiologyEvents(retentionPeriodDays)

        clearLegacyData()
    }

    private suspend fun updateIsolationConfiguration() {
        runCatching {
            val response = isolationConfigurationApi.getIsolationConfiguration()
            isolationConfigurationProvider.durationDays = response.durationDays
        }
    }

    private suspend fun updateRiskyVenueConfiguration() {
        runCatching {
            val response = riskyVenueConfigurationApi.getRiskyVenueConfiguration()
            riskyVenueConfigurationProvider.durationDays = response.durationDays
        }
    }

    private fun clearOldEpidemiologyEvents(retentionPeriodDays: Int) {
        epidemiologyEventProvider.clearOnAndBefore(LocalDate.now(clock).minusDays(retentionPeriodDays.toLong()))
    }

    private fun clearLegacyData() {
        exposureNotificationTokensProvider.clear()
    }
}
