package uk.nhs.nhsx.covid19.android.app.common

import com.jeroenmols.featureflag.framework.FeatureFlag.STORE_EXPOSURE_WINDOWS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider

class ClearOutdatedDataAndUpdateIsolationConfiguration @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val testResultsProvider: TestResultsProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val isolationConfigurationApi: IsolationConfigurationApi,
    private val exposureNotificationTokensProvider: ExposureNotificationTokensProvider,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val clock: Clock
) {

    suspend operator fun invoke(): Result<Unit> = runSafely {

        updateIsolationConfiguration()

        val expiryDays = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod

        val state = isolationStateMachine.readState()
        if (state is Default) {
            if (state.previousIsolation == null) {
                val today = LocalDate.now(clock)
                testResultsProvider.clearBefore(today.minusDays(expiryDays.toLong()))
            } else if (state.previousIsolation.expiryDate.isMoreThanOrExactlyDaysAgo(expiryDays)) {
                testResultsProvider.clearBefore(state.previousIsolation.expiryDate)
                isolationStateMachine.clearPreviousIsolation()
            }

            if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS)) {
                if (exposureNotificationTokensProvider.tokens.isEmpty()) {
                    epidemiologyEventProvider.clear()
                }
            }
        }
    }

    private suspend fun updateIsolationConfiguration() {
        runCatching {
            val response = isolationConfigurationApi.getIsolationConfiguration()
            isolationConfigurationProvider.durationDays = response.durationDays
        }
    }

    private fun LocalDate.isMoreThanOrExactlyDaysAgo(days: Int) =
        until(
            LocalDate.now(clock),
            ChronoUnit.DAYS
        ) >= days
}
