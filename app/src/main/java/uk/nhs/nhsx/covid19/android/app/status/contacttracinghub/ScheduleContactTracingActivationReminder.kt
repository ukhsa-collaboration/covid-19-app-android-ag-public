package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class ScheduleContactTracingActivationReminder @Inject constructor(
    private val exposureNotificationReminderAlarmController: ExposureNotificationReminderAlarmController,
    private val contactTracingActivationReminderProvider: ContactTracingActivationReminderProvider,
    private val clock: Clock
) {

    operator fun invoke(delay: Duration) {
        val alarmTime = Instant.now(clock).plus(delay)
        contactTracingActivationReminderProvider.reminder = ContactTracingActivationReminder(alarmTime.toEpochMilli())
        exposureNotificationReminderAlarmController.setup(alarmTime)
    }
}
