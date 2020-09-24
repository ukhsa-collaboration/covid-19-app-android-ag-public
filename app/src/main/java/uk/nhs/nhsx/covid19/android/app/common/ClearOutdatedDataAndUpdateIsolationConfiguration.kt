package uk.nhs.nhsx.covid19.android.app.common

import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ClearOutdatedDataAndUpdateIsolationConfiguration(
    private val stateStorage: StateStorage,
    private val testResultsProvider: TestResultsProvider,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val isolationConfigurationApi: IsolationConfigurationApi,
    private val clock: Clock
) {

    @Inject
    constructor(
        stateStorage: StateStorage,
        testResultsProvider: TestResultsProvider,
        isolationConfigurationProvider: IsolationConfigurationProvider,
        isolationConfigurationApi: IsolationConfigurationApi
    ) : this(
        stateStorage,
        testResultsProvider,
        isolationConfigurationProvider,
        isolationConfigurationApi,
        Clock.systemDefaultZone()
    )

    suspend fun doWork(): Result<Unit> = runSafely {

        updateIsolationConfiguration()

        val expiryDays = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod

        if (isOutdated(stateStorage.state, expiryDays)) {
            testResultsProvider.clear()
        }
    }

    private suspend fun updateIsolationConfiguration() {
        val response = isolationConfigurationApi.getIsolationConfiguration()
        isolationConfigurationProvider.durationDays = response.durationDays
    }

    private fun isOutdated(state: State, expirationDuration: Int): Boolean {
        return when (state) {
            is Default ->
                state.previousIsolation?.expiryDate?.isMoreThanDaysAgo(expirationDuration)
                    ?: false
            is Isolation -> state.expiryDate.isMoreThanDaysAgo(expirationDuration)
        }
    }

    private fun LocalDate.isMoreThanDaysAgo(days: Int) =
        until(
            LocalDate.now(clock),
            ChronoUnit.DAYS
        ) >= days
}
