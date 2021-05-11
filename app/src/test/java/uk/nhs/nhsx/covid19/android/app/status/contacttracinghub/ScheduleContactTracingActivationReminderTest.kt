package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.status.ResumeContactTracingNotificationTimeProvider
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ScheduleContactTracingActivationReminderTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val resumeContactTracingNotificationTimeProvider =
        mockk<ResumeContactTracingNotificationTimeProvider>(relaxed = true)
    private val exposureNotificationReminderAlarmController =
        mockk<ExposureNotificationReminderAlarmController>(relaxed = true)

    private val scheduleContactTracingActivationReminder = ScheduleContactTracingActivationReminder(
        exposureNotificationReminderAlarmController,
        resumeContactTracingNotificationTimeProvider,
        fixedClock
    )

    @Test
    fun `schedule exposure notification reminder sets alarm controller`() {
        val delay = Duration.ofHours(1)
        val alarmTime = Instant.now(fixedClock).plus(delay)

        scheduleContactTracingActivationReminder(delay)

        verify { resumeContactTracingNotificationTimeProvider setProperty "value" value eq(alarmTime.toEpochMilli()) }
        verify { exposureNotificationReminderAlarmController.setup(alarmTime) }
    }
}
