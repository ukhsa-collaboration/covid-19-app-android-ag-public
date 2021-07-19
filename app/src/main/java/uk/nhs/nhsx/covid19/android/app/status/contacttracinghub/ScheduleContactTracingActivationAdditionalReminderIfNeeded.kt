package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import java.time.Clock
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ScheduleContactTracingActivationAdditionalReminderIfNeeded @Inject constructor(
    private val exposureNotificationReminderAlarmController: ExposureNotificationReminderAlarmController,
    private val contactTracingActivationReminderProvider: ContactTracingActivationReminderProvider,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    operator fun invoke() {
        val activationReminder = contactTracingActivationReminderProvider.reminder ?: return

        // Prevent scheduling additional reminder twice
        if (activationReminder.hasAlreadyScheduledAdditionalReminder()) {
            contactTracingActivationReminderProvider.reminder = null
            return
        }

        val tomorrowAtFourPm =
            ZonedDateTime.now(clock)
                .plus(1, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS)
                .plus(16, ChronoUnit.HOURS)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant()

        exposureNotificationReminderAlarmController.setup(tomorrowAtFourPm)
        contactTracingActivationReminderProvider.reminder = ContactTracingActivationReminder(
            alarmTime = tomorrowAtFourPm.toEpochMilli(),
            additionalReminderCount = activationReminder.additionalReminderCount + 1
        )
    }
}
