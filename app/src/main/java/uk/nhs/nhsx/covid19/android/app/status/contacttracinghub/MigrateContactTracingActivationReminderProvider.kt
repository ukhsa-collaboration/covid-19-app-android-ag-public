package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import uk.nhs.nhsx.covid19.android.app.status.ResumeContactTracingNotificationTimeProvider
import javax.inject.Inject

class MigrateContactTracingActivationReminderProvider @Inject constructor(
    private val contactTracingActivationReminderProvider: ContactTracingActivationReminderProvider,
    private val resumeContactTracingNotificationTimeProvider: ResumeContactTracingNotificationTimeProvider
) {
    operator fun invoke() {
        val contactTracingReminderTime = resumeContactTracingNotificationTimeProvider.value
        if (contactTracingReminderTime != null) {
            contactTracingActivationReminderProvider.reminder =
                ContactTracingActivationReminder(alarmTime = contactTracingReminderTime)
            resumeContactTracingNotificationTimeProvider.value = null
        }
    }
}
