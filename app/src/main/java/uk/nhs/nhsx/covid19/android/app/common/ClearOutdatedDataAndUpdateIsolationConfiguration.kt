package uk.nhs.nhsx.covid19.android.app.common

import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ClearOutdatedDataAndUpdateIsolationConfiguration(
    private val stateStorage: StateStorage,
    private val testResultProvider: LatestTestResultProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val isolationConfigurationApi: IsolationConfigurationApi,
    private val clock: Clock
) {

    @Inject
    constructor(
        stateStorage: StateStorage,
        testResultProvider: LatestTestResultProvider,
        isolationConfigurationProvider: IsolationConfigurationProvider,
        isolationConfigurationApi: IsolationConfigurationApi
    ) : this(
        stateStorage,
        testResultProvider,
        isolationConfigurationProvider,
        isolationConfigurationApi,
        Clock.systemDefaultZone()
    )

    suspend fun doWork(): Result<Unit> = runSafely {

        updateIsolationConfiguration()

        val expiryDays = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod

        clearOutdatedStateHistory(expiryDays)

        clearOutdatedTestResults(expiryDays)
    }

    private suspend fun updateIsolationConfiguration() {
        val response = isolationConfigurationApi.getIsolationConfiguration()
        isolationConfigurationProvider.durationDays = response.durationDays
    }

    private fun clearOutdatedTestResults(expirationDuration: Int) {
        if (testResultProvider.latestTestResult?.testEndDate?.isMoreThanDaysAgo(expirationDuration) == true) {
            testResultProvider.latestTestResult = null
        }
    }

    private fun clearOutdatedStateHistory(expirationDuration: Int) {
        stateStorage.getHistory()
            .filter { !isOutdated(it, expirationDuration) }
            .let { list ->
                stateStorage.updateHistory(list)
            }
    }

    private fun isOutdated(state: State, expirationDuration: Int): Boolean {
        return when (state) {
            is Default -> false
            is Isolation -> state.expiryDate.isMoreThanDaysAgo(expirationDuration)
        }
    }

    private fun LocalDate.isMoreThanDaysAgo(days: Int) =
        until(
            LocalDate.now(clock),
            ChronoUnit.DAYS
        ) >= days

    private fun Instant.isMoreThanDaysAgo(days: Int) =
        until(
            Instant.now(clock),
            ChronoUnit.DAYS
        ) >= days
}
