package uk.nhs.nhsx.covid19.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingActivationReminderProvider
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ExposureNotificationReminderAlarmControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxUnitFun = true)
    private val contactTracingActivationReminderProvider =
        mockk<ContactTracingActivationReminderProvider>(relaxUnitFun = true)

    private val testSubject = ExposureNotificationReminderAlarmController(
        context,
        alarmManager,
        contactTracingActivationReminderProvider
    )

    @Test
    fun `setup schedules AlarmManager with appropriate time`() {
        val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
        val delay = Duration.ofHours(1)
        val alarmTime = Instant.now(fixedClock).plus(delay)

        testSubject.setup(alarmTime)

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime.toEpochMilli(),
                any()
            )
        }
    }

    @Test
    fun `cancel AlarmController with pending intent`() {
        mockkStatic(PendingIntent::class)
        val pendingIntent = mockk<PendingIntent>()
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pendingIntent

        testSubject.cancel()

        verify {
            contactTracingActivationReminderProvider setProperty "reminder" value null
            alarmManager.cancel(pendingIntent)
        }
    }

    @Test
    fun `cancel AlarmController without pending intent`() {
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns null

        testSubject.cancel()

        verify { contactTracingActivationReminderProvider setProperty "reminder" value null }
        verify(exactly = 0) { alarmManager.cancel(any<PendingIntent>()) }
    }
}
