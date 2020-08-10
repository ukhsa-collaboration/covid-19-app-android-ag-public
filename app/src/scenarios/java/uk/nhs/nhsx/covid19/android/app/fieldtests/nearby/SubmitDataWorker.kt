package uk.nhs.nhsx.covid19.android.app.fieldtests.nearby

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.CollectedKeysHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.ApiClient
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.MatchingResult
import uk.nhs.nhsx.covid19.android.app.fieldtests.notifications.NotificationHelper
import uk.nhs.nhsx.covid19.android.app.fieldtests.storage.ExperimentSettingsProvider
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Calendar

class SubmitDataWorker(val context: Context) {

    private val experimentSettingsProvider =
        ExperimentSettingsProvider(context)

    fun scheduleNewAndSubmit() {
        scheduleSubmit()
        submit()
    }

    fun cancelAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, SubmitDataBroadcastReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, REQUEST_CODE, intent, FLAG_UPDATE_CURRENT)
        }

        alarmManager.cancel(alarmIntent)
    }

    fun scheduleSubmit() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, SubmitDataBroadcastReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, REQUEST_CODE, intent, FLAG_UPDATE_CURRENT)
        }

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.SECOND, experimentSettingsProvider.automaticDetectionFrequency)
        }

        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, alarmIntent)
        alarmManager.setAlarmClock(alarmClockInfo, alarmIntent)
    }

    private fun submit() {
        val notificationHelper = NotificationHelper(context)
        val experimentSettingsProvider =
            ExperimentSettingsProvider(
                context
            )
        val teamId = experimentSettingsProvider.teamId
        val deviceName = experimentSettingsProvider.deviceName
        val experimentId = experimentSettingsProvider.getExperimentId()
        val configurations = experimentSettingsProvider.getConfigurations()
        GlobalScope.launch {
            try {
                notificationHelper.showNotification()
                val info = ApiClient.service.getExperimentInfo(teamId, experimentId)
                val collectedKeysHandler = CollectedKeysHandler(context)
                val exposureEvents = collectedKeysHandler.handle(configurations, info)
                exposureEvents.entries.forEach { (configuration, exposureEvents) ->
                    val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    val result = MatchingResult(timestamp, configuration, exposureEvents)
                    ApiClient.service.sendResult(teamId, experimentId, deviceName, result)
                }

                Timber.d("Exposure events: $exposureEvents")
            } catch (exception: Exception) {
                Timber.e(exception)
            } finally {
                notificationHelper.hideNotification()
            }
        }
    }

    companion object {
        const val REQUEST_CODE = 1337
    }
}
