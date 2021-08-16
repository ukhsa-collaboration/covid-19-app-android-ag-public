package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class IsolationHubReminderAlarmControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxUnitFun = true)
    private val isolationHubReminderTimeProvider = mockk<IsolationHubReminderTimeProvider>(relaxUnitFun = true)

    private val testSubject =
        IsolationHubReminderAlarmController(context, alarmManager, isolationHubReminderTimeProvider)

    @Before
    fun setUp() {
        mockkStatic(PendingIntent::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(PendingIntent::class)
    }

    @Test
    fun `setup schedules AlarmManager with appropriate time`() {
        val fixedClock = Clock.fixed(Instant.parse("2021-01-01T00:00:00Z"), ZoneOffset.UTC)
        val delay = Duration.ofDays(1)
        val alarmTimeMillis = Instant.now(fixedClock).plus(delay).toEpochMilli()

        val pendingIntent = mockk<PendingIntent>()
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pendingIntent

        testSubject.setup(alarmTimeMillis)

        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTimeMillis,
                pendingIntent
            )
        }
    }

    @Test
    fun `cancel AlarmController with pending intent`() {
        val pendingIntent = mockk<PendingIntent>()
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pendingIntent

        testSubject.cancel()

        verify {
            isolationHubReminderTimeProvider setProperty "value" value null
            alarmManager.cancel(pendingIntent)
        }
    }

    @Test
    fun `cancel AlarmController without pending intent`() {
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns null

        testSubject.cancel()

        verify { isolationHubReminderTimeProvider setProperty "value" value null }
        verify(exactly = 0) { alarmManager.cancel(any<PendingIntent>()) }
    }
}
