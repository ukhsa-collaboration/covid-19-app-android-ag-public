package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class IsolationHubReminderAlarmController @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val isolationHubReminderTimeProvider: IsolationHubReminderTimeProvider
) {

    fun setup(triggerAtMillis: Long) {
        Timber.d("Should fire at: ${DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(triggerAtMillis))}")

        val pendingIntent = getPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancel() {
        isolationHubReminderTimeProvider.value = null

        val pendingIntent = getPendingIntent(PendingIntent.FLAG_NO_CREATE)

        if (pendingIntent != null) {
            Timber.d("Isolation hub reminder cancelled")
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun getPendingIntent(flags: Int) = PendingIntent.getBroadcast(
        context,
        ISOLATION_HUB_REMINDER_ALARM_INTENT_ID,
        Intent(context, IsolationHubReminderReceiver::class.java),
        flags
    )

    companion object {
        private const val ISOLATION_HUB_REMINDER_ALARM_INTENT_ID = 1444
    }
}
