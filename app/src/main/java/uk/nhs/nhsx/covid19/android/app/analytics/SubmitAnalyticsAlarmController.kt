package uk.nhs.nhsx.covid19.android.app.analytics

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.TotalAlarmManagerBackgroundTasks
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.GLOBAL_SCOPE
import uk.nhs.nhsx.covid19.android.app.receiver.SubmitAnalyticsAlarmReceiver
import uk.nhs.nhsx.covid19.android.app.util.BroadcastProvider
import uk.nhs.nhsx.covid19.android.app.util.HasInternetConnectivity
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SubmitAnalyticsAlarmController @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val submitAnalytics: SubmitAnalytics,
    private val clock: Clock,
    private val broadcastProvider: BroadcastProvider,
    @Named(GLOBAL_SCOPE) private val globalScope: CoroutineScope,
    private val hasInternetConnectivity: HasInternetConnectivity
) {

    fun onDeviceRebooted() {
        Timber.d("onDeviceRebooted")
        submitAnalyticsAndSetupNext()
    }

    fun onAlarmTriggered() {
        Timber.d("onAlarmTriggered")
        submitAnalyticsAndSetupNext()
    }

    fun onAppCreated() {
        Timber.d("onAppCreated")
        if (!isAlarmScheduled()) {
            submitAnalyticsAndSetupNext()
        }
    }

    private fun isAlarmScheduled(): Boolean {
        return getExistingPendingIntent() != null
    }

    private fun submitAnalyticsAndSetupNext() {
        Timber.d("submitAnalyticsAndSetupNext")
        globalScope.launch {
            executeWithWakeLock {
                setupNextAlarm()
                if (hasInternetConnectivity()) {
                    analyticsEventProcessor.track(TotalAlarmManagerBackgroundTasks)
                    submitAnalytics()
                }
            }
        }
    }

    private fun setupNextAlarm() {
        val startAt = Instant.now(clock).plus(RECURRING_DURATION)
        Timber.d("setupNextAlarm at ${startAt.toISOSecondsFormat()}")

        val pendingIntent = broadcastProvider.getBroadcast(
            context,
            SUBMIT_ANALYTICS_ALARM_INTENT_ID,
            SubmitAnalyticsAlarmReceiver::class.java,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startAt.toEpochMilli(),
            pendingIntent
        )
    }

    fun cancelIfScheduled() {
        Timber.d("cancel")
        val pendingIntent = getExistingPendingIntent()

        if (pendingIntent != null) {
            Timber.d("alarm cancelled")
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun getExistingPendingIntent(): PendingIntent? {
        return broadcastProvider.getBroadcast(
            context,
            SUBMIT_ANALYTICS_ALARM_INTENT_ID,
            SubmitAnalyticsAlarmReceiver::class.java,
            PendingIntent.FLAG_NO_CREATE
        )
    }

    private suspend fun executeWithWakeLock(function: suspend () -> Unit) {
        val wakeLock: PowerManager.WakeLock =
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "NHS_COVID-19::SubmitAnalyticsAndSetUpNextAlarmWakelock"
                ).apply {
                    acquire(ANALYTICS_SUBMISSION_WAKE_LOCK_TIMEOUT)
                }
            }
        try {
            function()
        } finally {
            wakeLock.release()
        }
    }

    companion object {
        const val SUBMIT_ANALYTICS_ALARM_INTENT_ID = 1340
        private val RECURRING_DURATION = Duration.ofHours(2)
        private val ANALYTICS_SUBMISSION_WAKE_LOCK_TIMEOUT = Duration.ofMinutes(15).toMillis()
    }
}
