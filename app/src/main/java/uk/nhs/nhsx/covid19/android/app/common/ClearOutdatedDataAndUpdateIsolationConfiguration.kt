package uk.nhs.nhsx.covid19.android.app.common

import com.jeroenmols.featureflag.framework.FeatureFlag.STORE_EXPOSURE_WINDOWS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfoProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenueConfigurationApi
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ClearOutdatedDataAndUpdateIsolationConfiguration @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val relevantTestResultProvider: RelevantTestResultProvider,
    private val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val isolationConfigurationApi: IsolationConfigurationApi,
    private val riskyVenueConfigurationProvider: RiskyVenueConfigurationProvider,
    private val riskyVenueConfigurationApi: RiskyVenueConfigurationApi,
    private val exposureNotificationTokensProvider: ExposureNotificationTokensProvider,
    private val exposureCircuitBreakerInfoProvider: ExposureCircuitBreakerInfoProvider,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val clock: Clock
) {

    suspend operator fun invoke(): Result<Unit> = runSafely {
        updateIsolationConfiguration()
        updateRiskyVenueConfiguration()

        val expiryDays = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod

        val state = isolationStateMachine.readState()
        if (state is Default) {
            val previousIsolation = state.previousIsolation
            if (previousIsolation == null) {
                val oldestTestEndDateToKeep = Instant.now(clock).minus(expiryDays.toLong(), ChronoUnit.DAYS)
                if (relevantTestResultProvider.testResult?.testEndDate?.isBefore(oldestTestEndDateToKeep) == true) {
                    relevantTestResultProvider.clear()
                }
                clearOldUnacknowledgedTestResults(expiryDays)
            } else if (previousIsolation.expiryDate.isMoreThanOrExactlyDaysAgo(expiryDays)) {
                relevantTestResultProvider.clear()
                clearOldUnacknowledgedTestResults(expiryDays)
                isolationStateMachine.clearPreviousIsolation()
            }

            if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS)) {
                if (exposureCircuitBreakerInfoProvider.info.isEmpty()) {
                    epidemiologyEventProvider.clear()
                }
            }
        }

        if (!lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk())
            lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = null

        clearLegacyData()
    }

    private fun clearOldUnacknowledgedTestResults(expiryDays: Int) {
        unacknowledgedTestResultsProvider.clearBefore(LocalDate.now(clock).minusDays(expiryDays.toLong()))
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

    private fun clearLegacyData() {
        exposureNotificationTokensProvider.clear()
    }

    private fun LocalDate.isMoreThanOrExactlyDaysAgo(days: Int) =
        until(
            LocalDate.now(clock),
            ChronoUnit.DAYS
        ) >= days
}
