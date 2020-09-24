package uk.nhs.nhsx.covid19.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ExposureNotificationReminderAlarmControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = ExposureNotificationReminderAlarmController(
        context,
        alarmManager
    )

    @Test
    fun `setup calls AlarmManager with proper time`() {
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
    fun `cancel AlarmController`() {
        mockkStatic(PendingIntent::class)
        val pendingIntent = mockk<PendingIntent>()
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pendingIntent

        testSubject.cancel()
        verify {
            alarmManager.cancel(pendingIntent)
        }
    }
}
