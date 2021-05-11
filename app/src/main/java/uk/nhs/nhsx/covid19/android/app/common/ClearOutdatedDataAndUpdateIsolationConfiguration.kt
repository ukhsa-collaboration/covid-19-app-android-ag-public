package uk.nhs.nhsx.covid19.android.app.common

import com.jeroenmols.featureflag.framework.FeatureFlag.STORE_EXPOSURE_WINDOWS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenueConfigurationApi
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ClearOutdatedDataAndUpdateIsolationConfiguration @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
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

        val retentionPeriodDays = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod

        val state = isolationStateMachine.readLogicalState()
        if (!state.isActiveIsolation(clock)) {
            if (state is NeverIsolating) {
                val oldestTestEndDateToKeep = LocalDate.now(clock).minusDays(retentionPeriodDays.toLong())
                if (state.negativeTest?.testResult?.testEndDate?.isBefore(oldestTestEndDateToKeep) == true) {
                    isolationStateMachine.reset()
                }
                clearOldUnacknowledgedTestResults(retentionPeriodDays)
            } else if (state is PossiblyIsolating && state.expiryDate.isMoreThanOrExactlyDaysAgo(retentionPeriodDays)) {
                clearOldUnacknowledgedTestResults(retentionPeriodDays)
                isolationStateMachine.reset()
            }
        }

        if (!lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk()) {
            lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = null
        }

        clearOutdatedKeySharingInfo()

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

    private fun clearOldUnacknowledgedTestResults(expiryDays: Int) {
        unacknowledgedTestResultsProvider.clearBefore(LocalDate.now(clock).minusDays(expiryDays.toLong()))
    }

    private fun clearOldEpidemiologyEvents(retentionPeriodDays: Int) {
        if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS)) {
            epidemiologyEventProvider.clearOnAndBefore(LocalDate.now(clock).minusDays(retentionPeriodDays.toLong()))
        }
    }

    private fun clearLegacyData() {
        exposureNotificationTokensProvider.clear()
    }

    private fun LocalDate.isMoreThanOrExactlyDaysAgo(days: Int) =
        until(
            LocalDate.now(clock),
            ChronoUnit.DAYS
        ) >= days
}
