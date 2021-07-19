package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ScheduleContactTracingActivationReminderTest {

    private val contactTracingActivationReminderProvider =
        mockk<ContactTracingActivationReminderProvider>(relaxUnitFun = true)
    private val exposureNotificationReminderAlarmController =
        mockk<ExposureNotificationReminderAlarmController>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private val scheduleContactTracingActivationReminder = ScheduleContactTracingActivationReminder(
        exposureNotificationReminderAlarmController,
        contactTracingActivationReminderProvider,
        fixedClock
    )

    @Test
    fun `schedule exposure notification reminder sets alarm controller`() {
        val delay = Duration.ofHours(1)

        scheduleContactTracingActivationReminder(delay)

        val expectedAlarmTime = Instant.now(fixedClock).plus(delay)
        val expectedReminder = ContactTracingActivationReminder(
            alarmTime = expectedAlarmTime.toEpochMilli(),
            additionalReminderCount = 0
        )

        verify { contactTracingActivationReminderProvider setProperty "reminder" value eq(expectedReminder) }
        verify { exposureNotificationReminderAlarmController.setup(expectedAlarmTime) }
    }
}
