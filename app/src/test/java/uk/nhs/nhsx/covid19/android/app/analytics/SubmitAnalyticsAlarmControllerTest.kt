package uk.nhs.nhsx.covid19.android.app.analytics

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import io.mockk.MockKVerificationScope
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.TotalAlarmManagerBackgroundTasks
import uk.nhs.nhsx.covid19.android.app.receiver.SubmitAnalyticsAlarmReceiver
import uk.nhs.nhsx.covid19.android.app.util.BroadcastProvider
import uk.nhs.nhsx.covid19.android.app.util.HasInternetConnectivity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class SubmitAnalyticsAlarmControllerTest {

    private val context = mockk<Context>()
    private val alarmManager = mockk<AlarmManager>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val submitAnalyticsMock = mockk<SubmitAnalytics>()
    private val fixedClock = Clock.fixed(Instant.parse("2021-01-10T10:00:00Z"), ZoneOffset.UTC)
    private val broadcastProvider = mockk<BroadcastProvider>()
    private val pendingIntent = mockk<PendingIntent>()
    private val powerManager = mockk<PowerManager>()
    private val wakeLock = mockk<WakeLock>(relaxUnitFun = true)
    private val hasInternetConnectivity = mockk<HasInternetConnectivity>()

    private val testCoroutineScope = TestCoroutineScope()

    val testSubject = SubmitAnalyticsAlarmController(
        context,
        alarmManager,
        analyticsEventProcessor,
        submitAnalyticsMock,
        fixedClock,
        broadcastProvider,
        testCoroutineScope,
        hasInternetConnectivity
    )

    @Before
    fun setUp() {
        every { broadcastProvider.getBroadcast(any(), any(), any(), any()) } returns pendingIntent
        coEvery { hasInternetConnectivity.invoke() } returns true
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every {
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "NHS_COVID-19::SubmitAnalyticsAndSetUpNextAlarmWakelock"
            )
        } returns wakeLock
    }

    @Test
    fun `when device is rebooted should submit analytics`() {
        whenDeviceIsRebooted()

        coVerifyOrder {
            thenWakeLockIsAcquired()
            thenAlarmIsScheduled()
            thenAnalyticsEventIsTracked()
            thenAnalyticsAreSubmitted()
            thenWakeLockIsReleased()
        }
    }

    @Test
    fun `given no internet connectivity when device is rebooted do not submit analytics`() {
        givenNoInternetConnectivity()

        whenDeviceIsRebooted()

        verifyOrder {
            thenWakeLockIsAcquired()
            thenAlarmIsScheduled()
            thenWakeLockIsReleased()
        }

        thenNoAnalyticsAreSubmitted()
        thenNoAnalyticsEventIsTracked()
    }

    @Test
    fun `when app is created should submit analytics`() {
        givenNoAlarmIsScheduled()

        whenAppIsCreated()

        coVerifyOrder {
            thenWakeLockIsAcquired()
            thenAlarmIsScheduled()
            thenAnalyticsEventIsTracked()
            thenAnalyticsAreSubmitted()
            thenWakeLockIsReleased()
        }
    }

    @Test
    fun `given no internet connectivity when app is created do not submit analytics`() {
        givenNoInternetConnectivity()
        givenNoAlarmIsScheduled()

        whenAppIsCreated()

        verifyOrder {
            thenWakeLockIsAcquired()
            thenAlarmIsScheduled()
        }

        thenNoAnalyticsEventIsTracked()
        thenNoAnalyticsAreSubmitted()
    }

    @Test
    fun `when alarm is triggered submit analytics`() {
        whenAlarmIsTriggered()

        coVerifyOrder {
            thenWakeLockIsAcquired()
            thenAlarmIsScheduled()
            thenAnalyticsEventIsTracked()
            thenAnalyticsAreSubmitted()
            thenWakeLockIsReleased()
        }
    }

    @Test
    fun `given no internet connectivity when alarm is triggered do not submit analytics`() {
        givenNoInternetConnectivity()

        whenAlarmIsTriggered()

        verifyOrder {
            thenWakeLockIsAcquired()
            thenAlarmIsScheduled()
            thenWakeLockIsReleased()
        }

        thenNoAnalyticsEventIsTracked()
        thenNoAnalyticsAreSubmitted()
    }

    @Test
    fun `when app is created and alarm is already scheduled do not submit analytics`() {
        givenAlarmIsScheduled()

        whenAppIsCreated()

        thenNoAnalyticsAreSubmitted()
        thenNoAlarmIsScheduled()
    }

    @Test
    fun `on cancel cancels scheduled alarm`() {
        whenAlarmShouldBeCancelled()

        verifyOrder {
            thenCheckForExistingAlarm()
            thenAlarmIsCancelled()
        }

        thenNoAnalyticsAreSubmitted()
    }

    @Test
    fun `on cancel does nothing if no alarm is scheduled`() {
        givenNoAlarmIsScheduled()

        whenAlarmShouldBeCancelled()

        thenCheckForExistingAlarm()
        thenNoAnalyticsAreSubmitted()
        thenNoAlarmIsScheduled()
    }

    private fun givenNoAlarmIsScheduled() {
        every {
            broadcastProvider.getBroadcast(
                context,
                SubmitAnalyticsAlarmController.SUBMIT_ANALYTICS_ALARM_INTENT_ID,
                SubmitAnalyticsAlarmReceiver::class.java,
                PendingIntent.FLAG_NO_CREATE
            )
        } returns null
    }

    private fun givenAlarmIsScheduled() {
        every {
            broadcastProvider.getBroadcast(
                context,
                SubmitAnalyticsAlarmController.SUBMIT_ANALYTICS_ALARM_INTENT_ID,
                SubmitAnalyticsAlarmReceiver::class.java,
                PendingIntent.FLAG_NO_CREATE
            )
        } returns pendingIntent
    }

    private fun whenDeviceIsRebooted() {
        testSubject.onDeviceRebooted()
    }

    private fun givenNoInternetConnectivity() {
        coEvery { hasInternetConnectivity.invoke() } returns false
    }

    private fun whenAppIsCreated() {
        testSubject.onAppCreated()
    }

    private fun whenAlarmIsTriggered() {
        testSubject.onAlarmTriggered()
    }

    private fun whenAlarmShouldBeCancelled() {
        testSubject.cancelIfScheduled()
    }

    private suspend fun thenAnalyticsEventIsTracked() {
        analyticsEventProcessor.track(TotalAlarmManagerBackgroundTasks)
    }

    private fun thenNoAnalyticsEventIsTracked() {
        coVerify(exactly = 0) { analyticsEventProcessor.track(TotalAlarmManagerBackgroundTasks) }
    }

    private suspend fun MockKVerificationScope.thenAnalyticsAreSubmitted() {
        submitAnalyticsMock.invoke(any())
    }

    private fun thenNoAnalyticsAreSubmitted() {
        verify { submitAnalyticsMock wasNot called }
    }

    private fun thenCheckForExistingAlarm() {
        broadcastProvider.getBroadcast(
            context,
            SubmitAnalyticsAlarmController.SUBMIT_ANALYTICS_ALARM_INTENT_ID,
            SubmitAnalyticsAlarmReceiver::class.java,
            PendingIntent.FLAG_NO_CREATE
        )
    }

    private fun thenAlarmIsScheduled() {
        broadcastProvider.getBroadcast(
            context,
            SubmitAnalyticsAlarmController.SUBMIT_ANALYTICS_ALARM_INTENT_ID,
            SubmitAnalyticsAlarmReceiver::class.java,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val startIn2Hours = fixedClock.instant()
            .plus(2, ChronoUnit.HOURS)
            .toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startIn2Hours,
            pendingIntent
        )
    }

    private fun thenNoAlarmIsScheduled() {
        verify { alarmManager wasNot called }
    }

    private fun thenAlarmIsCancelled() {
        alarmManager.cancel(pendingIntent)
    }

    private fun thenWakeLockIsAcquired() {
        wakeLock.acquire(Duration.ofMinutes(15).toMillis())
    }

    private fun thenWakeLockIsReleased() {
        wakeLock.release()
    }
}
