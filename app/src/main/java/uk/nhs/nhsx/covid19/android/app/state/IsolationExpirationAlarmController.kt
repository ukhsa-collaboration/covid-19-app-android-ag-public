package uk.nhs.nhsx.covid19.android.app.state

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.receiver.ExpirationCheckReceiver
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsolationExpirationAlarmController @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager
) {

    fun setupExpirationCheck(expiryDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()) {
        val startAt = expiryDate
            .atStartOfDay()
            .atZone(zoneId)
            .minusHours(3)
            .toInstant()
            .toEpochMilli()

        val forPrinting = Instant.ofEpochMilli(startAt)
        Timber.d("Should fire at: ${DateTimeFormatter.ISO_INSTANT.format(forPrinting)}")

        val expirationCheckReceiver = PendingIntent.getBroadcast(
            context,
            EXPIRATION_ALARM_INTENT_ID,
            Intent(context, ExpirationCheckReceiver::class.java),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startAt,
            expirationCheckReceiver
        )
    }

    fun cancelExpirationCheckIfAny() {
        val pendingIntent = PendingIntent.getBroadcast(
            context, EXPIRATION_ALARM_INTENT_ID, Intent(context, ExpirationCheckReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            Timber.d("expiration alarm cancelled")
            alarmManager.cancel(pendingIntent)
        }
    }
    companion object {
        private const val EXPIRATION_ALARM_INTENT_ID = 1337
    }
}
