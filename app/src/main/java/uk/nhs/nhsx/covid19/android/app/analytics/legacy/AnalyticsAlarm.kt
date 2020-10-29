package uk.nhs.nhsx.covid19.android.app.analytics.legacy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsAggregatorReceiver
import javax.inject.Inject

@Deprecated("Use SubmitAnalytics, this is only for migration")
class AnalyticsAlarm @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager
) {

    fun cancel() {
        val analyticsAggregator = PendingIntent.getBroadcast(
            context,
            ANALYTICS_ALARM_INTENT_ID,
            Intent(context, AnalyticsAggregatorReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE
        )

        if (analyticsAggregator != null) {
            Timber.d("analytics alarm cancelled")
            alarmManager.cancel(analyticsAggregator)
        }
    }

    companion object {
        private const val ANALYTICS_ALARM_INTENT_ID = 1338
    }
}
