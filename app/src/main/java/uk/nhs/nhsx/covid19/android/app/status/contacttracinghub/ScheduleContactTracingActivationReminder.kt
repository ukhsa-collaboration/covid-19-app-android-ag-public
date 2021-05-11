package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.status.ResumeContactTracingNotificationTimeProvider
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class ScheduleContactTracingActivationReminder @Inject constructor(
    private val exposureNotificationReminderAlarmController: ExposureNotificationReminderAlarmController,
    private val resumeContactTracingNotificationTimeProvider: ResumeContactTracingNotificationTimeProvider,
    private val clock: Clock
) {

    operator fun invoke(delay: Duration) {
        val alarmTime = Instant.now(clock).plus(delay)
        resumeContactTracingNotificationTimeProvider.value = alarmTime.toEpochMilli()
        exposureNotificationReminderAlarmController.setup(alarmTime)
    }
}
