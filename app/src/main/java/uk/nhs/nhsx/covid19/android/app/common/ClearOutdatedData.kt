@file:Suppress("DEPRECATION")
package uk.nhs.nhsx.covid19.android.app.common

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import javax.inject.Inject

class ClearOutdatedData @Inject constructor(
    private val resetIsolationStateIfNeeded: ResetIsolationStateIfNeeded,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val clearOutdatedKeySharingInfo: ClearOutdatedKeySharingInfo,
    private val clearOutdatedTestOrderPollingConfigs: ClearOutdatedTestOrderPollingConfigs,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val exposureNotificationTokensProvider: ExposureNotificationTokensProvider,
    private val analyticsLogStorage: AnalyticsLogStorage,
    private val clock: Clock
) {

    operator fun invoke() {
        runCatching {
            Timber.d("Clearing outdated data")

            resetIsolationStateIfNeeded()

            if (!lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk()) {
                lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = null
            }

            clearOutdatedKeySharingInfo()
            clearOutdatedTestOrderPollingConfigs()

            val retentionPeriodDays = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
            clearOldEpidemiologyEvents(retentionPeriodDays)
            clearOutdatedAnalyticsLogs(retentionPeriodDays)

            clearLegacyData()
        }
    }

    private fun clearOldEpidemiologyEvents(retentionPeriodDays: Int) {
        epidemiologyEventProvider.clearOnAndBefore(LocalDate.now(clock).minusDays(retentionPeriodDays.toLong()))
    }

    private fun clearOutdatedAnalyticsLogs(retentionPeriodDays: Int) {
        val startOfToday = Instant.now(clock).truncatedTo(DAYS)
        val outdatedThreshold = startOfToday.minus(retentionPeriodDays.toLong(), DAYS)
        analyticsLogStorage.removeBeforeOrEqual(outdatedThreshold)
    }

    private fun clearLegacyData() {
        exposureNotificationTokensProvider.clear()
    }
}
