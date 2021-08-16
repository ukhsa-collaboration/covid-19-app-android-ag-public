package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ScheduleIsolationHubReminder @Inject constructor(
    private val isolationHubReminderAlarmController: IsolationHubReminderAlarmController,
    private val isolationHubReminderTimeProvider: IsolationHubReminderTimeProvider,
    private val clock: Clock
) {
    operator fun invoke() {
        val inExactly24HoursInMillis = Instant.now(clock)
            .plus(1, ChronoUnit.DAYS)
            .toEpochMilli()

        isolationHubReminderTimeProvider.value = inExactly24HoursInMillis
        isolationHubReminderAlarmController.setup(inExactly24HoursInMillis)
    }
}
