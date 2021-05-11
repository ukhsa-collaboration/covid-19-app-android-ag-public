package uk.nhs.nhsx.covid19.android.app.state

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.receiver.ExpirationCheckReceiver
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationAlarmController.Companion.EXPIRATION_ALARM_INTENT_ID
import uk.nhs.nhsx.covid19.android.app.util.BroadcastProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class IsolationExpirationAlarmControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val isolationExpirationAlarmProvider = mockk<IsolationExpirationAlarmProvider>(relaxUnitFun = true)
    private val broadcastProvider = mockk<BroadcastProvider>()
    private val calculateExpirationNotificationTime = mockk<CalculateExpirationNotificationTime>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-18T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject = IsolationExpirationAlarmController(
        context,
        alarmManager,
        isolationExpirationAlarmProvider,
        broadcastProvider,
        calculateExpirationNotificationTime,
        fixedClock
    )

    private val pendingIntent = mockk<PendingIntent>()

    private val zone = ZoneId.of("Europe/London")

    @Before
    fun setUp() {
        every { broadcastProvider.getBroadcast(any(), any(), any(), any()) } returns pendingIntent
    }

    @Test
    fun `onDeviceRebooted with isolationExpirationAlarmProvider returning expiry date schedules alarm`() {
        val alarmTime = Instant.parse("2020-07-19T20:00:00Z").toEpochMilli()
        every { isolationExpirationAlarmProvider.value } returns alarmTime

        testSubject.onDeviceRebooted()

        verify {
            broadcastProvider.getBroadcast(
                context,
                EXPIRATION_ALARM_INTENT_ID,
                ExpirationCheckReceiver::class.java,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        }
        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
        }
        verify(exactly = 0) { isolationExpirationAlarmProvider.value = any() }
    }

    @Test
    fun `onDeviceRebooted with empty isolationExpirationAlarmProvider does not schedule alarm`() {
        every { isolationExpirationAlarmProvider.value } returns null

        testSubject.onDeviceRebooted()

        verify(exactly = 0) { isolationExpirationAlarmProvider.value = any() }
        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `setupExpirationCheck with new expired isolation state and current state not in isolation does not schedule alarm`() {
        val expiryDate = LocalDate.now(fixedClock)

        val currentState = isolationHelper.neverInIsolation().asLogical()
        val newIsolation = isolationHelper.selfAssessment(
            selfAssessmentDate = LocalDate.parse("2020-07-18"),
            expiryDate = expiryDate
        ).asIsolation().asLogical()

        testSubject.setupExpirationCheck(currentState, newIsolation, zone)

        verify(exactly = 0) { isolationExpirationAlarmProvider.value = any() }
        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `setupExpirationCheck with new isolation state and current state not in isolation schedules alarm with proper time`() {
        val expiryDate = LocalDate.parse("2020-07-20")

        val currentState = isolationHelper.neverInIsolation().asLogical()
        val newIsolation = isolationHelper.selfAssessment(
            selfAssessmentDate = LocalDate.parse("2020-07-18"),
            expiryDate = expiryDate
        ).asIsolation().asLogical()

        val alarmInstant = Instant.parse("2020-07-19T20:00:00Z")
        every { calculateExpirationNotificationTime(expiryDate, zone) } returns alarmInstant

        testSubject.setupExpirationCheck(currentState, newIsolation, zone)

        val alarmTime = alarmInstant.toEpochMilli()

        verify {
            broadcastProvider.getBroadcast(
                context,
                EXPIRATION_ALARM_INTENT_ID,
                ExpirationCheckReceiver::class.java,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        }
        verify { isolationExpirationAlarmProvider.value = alarmTime }
        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
        }
    }

    @Test
    fun `setupExpirationCheck with new isolation state, current state in isolation and different expiry dates schedules alarm with proper time`() {
        val oldExpiryDate = LocalDate.parse("2020-07-19")
        val newExpiryDate = LocalDate.parse("2020-07-20")

        val currentState = isolationHelper.selfAssessment(
            selfAssessmentDate = LocalDate.parse("2020-07-18"),
            expiryDate = oldExpiryDate
        ).asIsolation().asLogical()

        val newIsolation = isolationHelper.selfAssessment(
            selfAssessmentDate = LocalDate.parse("2020-07-18"),
            expiryDate = newExpiryDate
        ).asIsolation().asLogical()

        val alarmInstant = Instant.parse("2020-07-19T20:00:00Z")
        every { calculateExpirationNotificationTime(newExpiryDate, zone) } returns alarmInstant

        testSubject.setupExpirationCheck(currentState, newIsolation, zone)

        val alarmTime = alarmInstant.toEpochMilli()

        verify {
            broadcastProvider.getBroadcast(
                context,
                EXPIRATION_ALARM_INTENT_ID,
                ExpirationCheckReceiver::class.java,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        }
        verify { isolationExpirationAlarmProvider.value = alarmTime }
        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
        }
    }

    @Test
    fun `setupExpirationCheck with new isolation state, current state in isolation and same expiry dates does not schedule alarm`() {
        val expiryDate = LocalDate.parse("2020-07-20")

        val currentState = isolationHelper.selfAssessment(
            selfAssessmentDate = LocalDate.parse("2020-07-18"),
            expiryDate = expiryDate
        ).asIsolation().asLogical()

        val newIsolation = isolationHelper.selfAssessment(
            selfAssessmentDate = LocalDate.parse("2020-07-18"),
            expiryDate = expiryDate
        ).asIsolation().asLogical()

        testSubject.setupExpirationCheck(currentState, newIsolation, zone)

        verify(exactly = 0) { isolationExpirationAlarmProvider.value = any() }
        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `cancelExpirationCheckIfAny cancels alarm`() {
        testSubject.cancelExpirationCheckIfAny()

        verify {
            broadcastProvider.getBroadcast(
                context,
                EXPIRATION_ALARM_INTENT_ID,
                ExpirationCheckReceiver::class.java,
                PendingIntent.FLAG_NO_CREATE
            )
        }
        verify { isolationExpirationAlarmProvider.value = null }
        verify { alarmManager.cancel(pendingIntent) }
    }
}
