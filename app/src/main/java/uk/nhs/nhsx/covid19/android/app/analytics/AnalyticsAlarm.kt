package uk.nhs.nhsx.covid19.android.app.analytics

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import com.jeroenmols.featureflag.framework.TestSetting
import timber.log.Timber
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class AnalyticsAlarm @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val getAnalyticsWindow: GetAnalyticsWindow
) {

    fun scheduleNextAnalyticsAggregator() {

        val startAt =
            if (RuntimeBehavior.isFeatureEnabled(TestSetting.DEBUG_ANALYTICS)) {
                Instant.now().plus(15, ChronoUnit.MINUTES)
            } else {
                getAnalyticsWindow.getCurrentWindowEnd()
            }

        Timber.d("Should fire at: ${DateTimeFormatter.ISO_INSTANT.format(startAt)}")

        val analyticsAggregator = PendingIntent.getBroadcast(
            context,
            ANALYTICS_ALARM_INTENT_ID,
            Intent(context, AnalyticsAggregatorReceiver::class.java),
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startAt.toEpochMilli(),
            analyticsAggregator
        )
    }

    companion object {
        private const val ANALYTICS_ALARM_INTENT_ID = 1338
    }
}
