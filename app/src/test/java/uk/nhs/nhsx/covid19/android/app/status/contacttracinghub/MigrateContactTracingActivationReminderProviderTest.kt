package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.status.ResumeContactTracingNotificationTimeProvider

class MigrateContactTracingActivationReminderProviderTest {

    private val contactTracingActivationReminderProvider =
        mockk<ContactTracingActivationReminderProvider>(relaxUnitFun = true)
    private val resumeContactTracingActivationReminderProvider =
        mockk<ResumeContactTracingNotificationTimeProvider>(relaxUnitFun = true)

    val migrateContactTracingActivationReminderProvider = MigrateContactTracingActivationReminderProvider(
        contactTracingActivationReminderProvider,
        resumeContactTracingActivationReminderProvider
    )

    @Test
    fun `perform migration when ResumeContactTracingNotificationTimeProvider contains alarm time`() {
        val expectedAlarmTimeInMs = 123L
        every { resumeContactTracingActivationReminderProvider.value } returns expectedAlarmTimeInMs

        migrateContactTracingActivationReminderProvider()

        val expectedReminder =
            ContactTracingActivationReminder(alarmTime = expectedAlarmTimeInMs, additionalReminderCount = 0)

        verify {
            contactTracingActivationReminderProvider setProperty "reminder" value eq(expectedReminder)
            resumeContactTracingActivationReminderProvider setProperty "value" value null
        }
    }

    @Test
    fun `do nothing when ResumeContactTracingNotificationTimeProvider is empty`() {
        every { resumeContactTracingActivationReminderProvider.value } returns null

        migrateContactTracingActivationReminderProvider()

        verify { resumeContactTracingActivationReminderProvider getProperty "value" }
        confirmVerified(contactTracingActivationReminderProvider, resumeContactTracingActivationReminderProvider)
    }
}
