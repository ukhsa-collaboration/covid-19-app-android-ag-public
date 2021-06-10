package uk.nhs.nhsx.covid19.android.app.state

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.receiver.ExpirationCheckReceiver
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.util.BroadcastProvider
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsolationExpirationAlarmController @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val isolationExpirationAlarmProvider: IsolationExpirationAlarmProvider,
    private val broadcastProvider: BroadcastProvider,
    private val calculateExpirationNotificationTime: CalculateExpirationNotificationTime,
    private val clock: Clock
) {

    fun onDeviceRebooted() {
        isolationExpirationAlarmProvider.value?.let {
            setupExpirationCheck(it)
        }
    }

    fun setupExpirationCheck(
        currentState: IsolationLogicalState,
        newIsolation: IsolationLogicalState
    ) {
        if (newIsolation !is PossiblyIsolating ||
            newIsolation.hasExpired(clock) ||
            (currentState is PossiblyIsolating && currentState.expiryDate == newIsolation.expiryDate)
        ) {
            return
        }

        val startAt = calculateExpirationNotificationTime(newIsolation.expiryDate)
            .toEpochMilli()

        isolationExpirationAlarmProvider.value = startAt

        setupExpirationCheck(startAt)
    }

    fun cancelExpirationCheckIfAny() {
        val pendingIntent = broadcastProvider.getBroadcast(
            context,
            EXPIRATION_ALARM_INTENT_ID,
            ExpirationCheckReceiver::class.java,
            PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            Timber.d("expiration alarm cancelled")

            isolationExpirationAlarmProvider.value = null

            alarmManager.cancel(pendingIntent)
        }
    }

    private fun setupExpirationCheck(startAt: Long) {
        val forPrinting = Instant.ofEpochMilli(startAt)
        Timber.d("Should fire at: ${DateTimeFormatter.ISO_INSTANT.format(forPrinting)}")

        val pendingIntent = broadcastProvider.getBroadcast(
            context,
            EXPIRATION_ALARM_INTENT_ID,
            ExpirationCheckReceiver::class.java,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startAt,
            pendingIntent
        )
    }

    companion object {
        internal const val EXPIRATION_ALARM_INTENT_ID = 1337
    }
}
