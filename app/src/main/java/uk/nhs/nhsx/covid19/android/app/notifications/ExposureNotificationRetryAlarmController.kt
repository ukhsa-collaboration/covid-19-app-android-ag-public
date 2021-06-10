package uk.nhs.nhsx.covid19.android.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.RiskyContactReminderNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider
import uk.nhs.nhsx.covid19.android.app.receiver.ExposureNotificationRetryReceiver
import uk.nhs.nhsx.covid19.android.app.util.BroadcastProvider
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExposureNotificationRetryAlarmController @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val notificationProvider: NotificationProvider,
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val analyticsEventTracker: AnalyticsEventTracker,
    private val clock: Clock,
    private val broadcastProvider: BroadcastProvider,
) {

    fun onDeviceRebooted() {
        Timber.d("onDeviceRebooted")
        showNotificationAndSetupNext()
    }

    fun onAlarmTriggered() {
        Timber.d("onAlarmTriggered")
        showNotificationAndSetupNext()
    }

    fun onAppCreated() {
        Timber.d("onAppCreated")
        if (!isAlarmScheduled()) {
            showNotificationAndSetupNext()
        }
    }

    private fun isAlarmScheduled(): Boolean {
        return getExistingPendingIntent() != null
    }

    private fun showNotificationAndSetupNext() {
        Timber.d("showNotificationAndSetupNext")
        if (shouldShowEncounterDetectionActivityProvider.value == true) {
            Timber.d("showing notification")
            notificationProvider.showExposureNotification()
            analyticsEventTracker.track(RiskyContactReminderNotification)
            setupNextAlarm()
        }
    }

    fun setupNextAlarm() {
        Timber.d("setupNextAlarm")
        val startAt: Long = Instant.now(clock)
            .plus(4, ChronoUnit.HOURS)
            .toEpochMilli()

        val pendingIntent = broadcastProvider.getBroadcast(
            context,
            EXPOSURE_NOTIFICATION_RETRY_ALARM_INTENT_ID,
            ExposureNotificationRetryReceiver::class.java,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startAt,
            pendingIntent
        )
    }

    fun cancel() {
        Timber.d("cancel")
        val pendingIntent = getExistingPendingIntent()

        if (pendingIntent != null) {
            Timber.d("alarm cancelled")
            alarmManager.cancel(pendingIntent)
        }

        notificationProvider.cancelExposureNotification()
    }

    private fun getExistingPendingIntent(): PendingIntent? {
        return broadcastProvider.getBroadcast(
            context,
            EXPOSURE_NOTIFICATION_RETRY_ALARM_INTENT_ID,
            ExposureNotificationRetryReceiver::class.java,
            PendingIntent.FLAG_NO_CREATE
        )
    }

    companion object {
        const val EXPOSURE_NOTIFICATION_RETRY_ALARM_INTENT_ID = 1339
    }
}
