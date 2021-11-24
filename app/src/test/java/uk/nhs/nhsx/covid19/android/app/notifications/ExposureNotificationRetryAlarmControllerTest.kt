package uk.nhs.nhsx.covid19.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.RiskyContactReminderNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController.Companion.EXPOSURE_NOTIFICATION_RETRY_ALARM_INTENT_ID
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider
import uk.nhs.nhsx.covid19.android.app.receiver.ExposureNotificationRetryReceiver
import uk.nhs.nhsx.covid19.android.app.util.BroadcastProvider
import java.time.Instant
import java.time.temporal.ChronoUnit

class ExposureNotificationRetryAlarmControllerTest {

    private val context = mockk<Context>()
    private val alarmManager = mockk<AlarmManager>(relaxUnitFun = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxUnitFun = true)
    private val shouldShowEncounterDetectionActivityProvider =
        mockk<ShouldShowEncounterDetectionActivityProvider>()
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val clock = mockk<DeviceClock>(relaxUnitFun = true)
    private val broadcastProvider = mockk<BroadcastProvider>()
    private val pendingIntent = mockk<PendingIntent>()

    private val testSubject = ExposureNotificationRetryAlarmController(
        context,
        alarmManager,
        notificationProvider,
        shouldShowEncounterDetectionActivityProvider,
        analyticsEventProcessor,
        clock,
        broadcastProvider,
    )

    @Before
    fun setUp() {
        every { broadcastProvider.getBroadcast(any(), any(), any(), any()) } returns pendingIntent
        every { clock.instant() } returns Instant.parse("2021-01-10T10:00:00Z")
    }

    @Test
    fun `when device is rebooted and should not show encounter detection activity do not show notification and do not schedule a new alarm`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns false

        testSubject.onDeviceRebooted()

        verify { notificationProvider wasNot called }
        verify { alarmManager wasNot called }
    }

    @Test
    fun `when device is rebooted and should show encounter detection activity show notification and schedule a new alarm`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns true

        testSubject.onDeviceRebooted()

        verify { notificationProvider.showExposureNotification() }
        verify { analyticsEventProcessor.track(RiskyContactReminderNotification) }
        verifyAlarmScheduled()
    }

    @Test
    fun `when app is created and should not show encounter detection activity do not show notification and do not schedule a new alarm`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns false

        testSubject.onAppCreated()

        verify { notificationProvider wasNot called }
        verify { alarmManager wasNot called }
    }

    @Test
    fun `when app is created and should show encounter detection activity and alarm is not scheduled show notification and schedule a new alarm`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns true
        every {
            broadcastProvider.getBroadcast(
                context,
                EXPOSURE_NOTIFICATION_RETRY_ALARM_INTENT_ID,
                ExposureNotificationRetryReceiver::class.java,
                PendingIntent.FLAG_NO_CREATE
            )
        } returns null

        testSubject.onAppCreated()

        verify { notificationProvider.showExposureNotification() }
        verify { analyticsEventProcessor.track(RiskyContactReminderNotification) }
        verifyAlarmScheduled()
    }

    @Test
    fun `when app is created and should show encounter detection activity and alarm is already scheduled do not show notification and do not schedule a new alarm`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns true
        every {
            broadcastProvider.getBroadcast(
                context,
                EXPOSURE_NOTIFICATION_RETRY_ALARM_INTENT_ID,
                ExposureNotificationRetryReceiver::class.java,
                PendingIntent.FLAG_NO_CREATE
            )
        } returns pendingIntent

        testSubject.onAppCreated()

        verify { notificationProvider wasNot called }
        verify { alarmManager wasNot called }
    }

    @Test
    fun `when alarm is triggered and should not show encounter detection activity do not show notification and do not schedule a new alarm`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns false

        testSubject.onAlarmTriggered()

        verify { notificationProvider wasNot called }
        verify { alarmManager wasNot called }
    }

    @Test
    fun `when alarm is triggered and should show encounter detection activity show notification and schedule a new alarm`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns true

        testSubject.onAlarmTriggered()

        verify { notificationProvider.showExposureNotification() }
        verify { analyticsEventProcessor.track(RiskyContactReminderNotification) }
        verifyAlarmScheduled()
    }

    @Test
    fun `on setupNextAlarm schedule a new alarm`() {
        testSubject.setupNextAlarm()

        verify { notificationProvider wasNot called }
        verifyAlarmScheduled()
    }

    @Test
    fun `on cancel cancels alarm`() {
        testSubject.cancel()

        verify {
            broadcastProvider.getBroadcast(
                context,
                EXPOSURE_NOTIFICATION_RETRY_ALARM_INTENT_ID,
                ExposureNotificationRetryReceiver::class.java,
                PendingIntent.FLAG_NO_CREATE
            )
        }

        verify { notificationProvider.cancelExposureNotification() }
        verify { alarmManager.cancel(pendingIntent) }
    }

    private fun verifyAlarmScheduled() {
        verify {
            broadcastProvider.getBroadcast(
                context,
                EXPOSURE_NOTIFICATION_RETRY_ALARM_INTENT_ID,
                ExposureNotificationRetryReceiver::class.java,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val startIn4Hours = clock.instant()
            .plus(4, ChronoUnit.HOURS)
            .toEpochMilli()
        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                startIn4Hours,
                pendingIntent
            )
        }
    }
}
