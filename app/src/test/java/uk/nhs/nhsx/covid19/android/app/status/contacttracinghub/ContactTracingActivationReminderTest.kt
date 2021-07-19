package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactTracingActivationReminderTest {

    @Test
    fun `when additionalReminderCount is more than zero then hasAlreadyScheduledAdditionalReminder returns true`() {
        val reminder = ContactTracingActivationReminder(alarmTime = 123L, additionalReminderCount = 1)

        assertTrue(reminder.hasAlreadyScheduledAdditionalReminder())
    }

    @Test
    fun `when additionalReminderCount is zero then hasAlreadyScheduledAdditionalReminder returns false`() {
        val reminder = ContactTracingActivationReminder(alarmTime = 123L, additionalReminderCount = 0)

        assertFalse(reminder.hasAlreadyScheduledAdditionalReminder())
    }
}
