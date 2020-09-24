package uk.nhs.nhsx.covid19.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.receiver.ExposureNotificationReminderReceiver
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExposureNotificationReminderAlarmController @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun setup(alarmTime: Instant) {
        val startAt = alarmTime.toEpochMilli()

        val forPrinting = Instant.ofEpochMilli(startAt)
        Timber.d("Should fire at: ${DateTimeFormatter.ISO_INSTANT.format(forPrinting)}")

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            EXPOSURE_NOTIFICATION_REMINDER_ALARM_INTENT_ID,
            Intent(context, ExposureNotificationReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startAt,
            pendingIntent
        )
    }

    fun cancel() {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            EXPOSURE_NOTIFICATION_REMINDER_ALARM_INTENT_ID,
            Intent(context, ExposureNotificationReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE
        )

        if (pendingIntent != null) {
            Timber.d("expiration alarm cancelled")
            alarmManager.cancel(pendingIntent)
        }
    }

    companion object {
        private const val EXPOSURE_NOTIFICATION_REMINDER_ALARM_INTENT_ID = 1338
    }
}
