package uk.nhs.nhsx.covid19.android.app.common

import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ResetIsolationStateIfNeeded @Inject constructor(
    val isolationStateMachine: IsolationStateMachine,
    val unacknowledgedTestResultsProvider: UnacknowledgedTestResultsProvider,
    val isolationConfigurationProvider: IsolationConfigurationProvider,
    val clock: Clock
) {
    operator fun invoke() {
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
    }

    private fun clearOldUnacknowledgedTestResults(expiryDays: Int) {
        unacknowledgedTestResultsProvider.clearBefore(LocalDate.now(clock).minusDays(expiryDays.toLong()))
    }

    private fun LocalDate.isMoreThanOrExactlyDaysAgo(days: Int) =
        until(
            LocalDate.now(clock),
            ChronoUnit.DAYS
        ) >= days
}
