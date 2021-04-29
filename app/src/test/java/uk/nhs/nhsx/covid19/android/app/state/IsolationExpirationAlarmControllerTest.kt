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
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationAlarmController.Companion.EXPIRATION_ALARM_INTENT_ID
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.util.BroadcastProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class IsolationExpirationAlarmControllerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val isolationExpirationAlarmProvider = mockk<IsolationExpirationAlarmProvider>(relaxUnitFun = true)
    private val broadcastProvider = mockk<BroadcastProvider>()

    private val testSubject = IsolationExpirationAlarmController(
        context,
        alarmManager,
        isolationExpirationAlarmProvider,
        broadcastProvider
    )

    val pendingIntent = mockk<PendingIntent>()

    private val zone = ZoneId.of("Europe/London")

    private val durationDays =
        DurationDays(
            contactCase = 14,
            indexCaseSinceSelfDiagnosisOnset = 5,
            indexCaseSinceSelfDiagnosisUnknownOnset = 3,
            maxIsolation = 21,
            pendingTasksRetentionPeriod = 14
        )

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
    fun `setupExpirationCheck with new isolations state and current state not in isolation schedules alarm with proper time`() {
        val expiryDate = LocalDate.parse("2020-07-20")

        val currentState = Default()
        val newIsolation = Isolation(
            isolationStart = Instant.parse("2020-07-18T00:00:00Z"),
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-18"),
                expiryDate = expiryDate,
                selfAssessment = true
            )
        )

        testSubject.setupExpirationCheck(currentState, newIsolation, zone)

        val alarmTime = Instant.parse("2020-07-19T20:00:00Z").toEpochMilli()

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
    fun `setupExpirationCheck with new isolations state, current state in isolation and different expiry dates schedules alarm with proper time`() {
        val oldExpiryDate = LocalDate.parse("2020-07-19")
        val newExpiryDate = LocalDate.parse("2020-07-20")

        val currentState = Isolation(
            isolationStart = Instant.parse("2020-07-18T00:00:00Z"),
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-18"),
                expiryDate = oldExpiryDate,
                selfAssessment = true
            )
        )
        val newIsolation = Isolation(
            isolationStart = Instant.parse("2020-07-18T00:00:00Z"),
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-18"),
                expiryDate = newExpiryDate,
                selfAssessment = true
            )
        )

        testSubject.setupExpirationCheck(currentState, newIsolation, zone)

        val alarmTime = Instant.parse("2020-07-19T20:00:00Z").toEpochMilli()

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
    fun `setupExpirationCheck with new isolations state, current state in isolation and same expiry dates does not schedule alarm`() {
        val expiryDate = LocalDate.parse("2020-07-20")

        val currentState = Isolation(
            isolationStart = Instant.parse("2020-07-18T00:00:00Z"),
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-18"),
                expiryDate = expiryDate,
                selfAssessment = true
            )
        )
        val newIsolation = Isolation(
            isolationStart = Instant.parse("2020-07-18T00:00:00Z"),
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-07-18"),
                expiryDate = expiryDate,
                selfAssessment = true
            )
        )

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
