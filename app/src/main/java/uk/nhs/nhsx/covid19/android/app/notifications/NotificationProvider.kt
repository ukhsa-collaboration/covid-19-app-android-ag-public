package uk.nhs.nhsx.covid19.android.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityActivity
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import javax.inject.Inject

class NotificationProvider @Inject constructor(private val context: Context) {

    init {
        createAreaRiskChangedNotificationChannel()
        createIsolationStateNotificationChannel()
        createTestResultsNotificationChannel()
        createAppAvailabilityNotificationChannel()
        createAppConfigurationNotificationChannel()
        createBackgroundWorkNotificationChannel()
    }

    companion object {
        const val RISK_CHANGED_CHANNEL_ID = "AREA_RISK_CHANGED"
        const val ISOLATION_STATE_CHANNEL_ID = "ISOLATION_STATE"
        const val TEST_RESULTS_CHANNEL_ID = "TEST_RESULTS"
        const val APP_AVAILABILITY_CHANNEL_ID = "APP_AVAILABILITY"
        const val APP_CONFIGURATION_CHANNEL_ID = "APP_CONFIGURATION"
        const val BACKGROUND_WORK_CHANNEL_ID = "BACKGROUND_WORK"
        const val AREA_RISK_CHANGED_NOTIFICATION_ID = 0
        const val RISKY_VENUE_VISIT_NOTIFICATION_ID = 1
        const val STATE_EXPIRATION_NOTIFICATION_ID = 2
        const val STATE_EXPOSURE_NOTIFICATION_ID = 3
        const val TEST_RESULTS_NOTIFICATION_ID = 4
        const val APP_AVAILABLE_NOTIFICATION_ID = 5
        const val APP_NOT_AVAILABLE_NOTIFICATION_ID = 6
        const val EXPOSURE_REMINDER_NOTIFICATION_ID = 7
        const val POTENTIAL_EXPOSURE_EXPLANATION_NOTIFICATION_ID = 8

        const val REQUEST_CODE_APP_IS_NOT_AVAILABLE = 1
        const val REQUEST_CODE_APP_IS_AVAILABLE = 2
        const val REQUEST_CODE_NOTIFICATION_REMINDER_CONTENT_INTENT = 3
        const val REQUEST_CODE_NOTIFICATION_REMINDER_ACTION_INTENT = 4
        const val REQUEST_CODE_SHOW_TEST_RESULTS = 5
        const val REQUEST_CODE_SHOW_EXPOSURE_NOTIFICATION = 6
        const val REQUEST_CODE_SHOW_STATE_EXPIRATION_NOTIFICATION = 7
        const val REQUEST_CODE_SHOW_RISKY_VENUE_VISIT_NOTIFICATION = 8
        const val REQUEST_CODE_SHOW_AREA_RISK_CHANGED_NOTIFICATION = 9
        const val REQUEST_CODE_UPDATING_DATABASE_NOTIFICATION = 10
        const val REQUEST_CODE_POTENTIAL_EXPOSURE_EXPLANATION_NOTIFICATION = 11

        // TODO ?maybe move to StatusActivity
        const val TAP_EXPOSURE_NOTIFICATION_REMINDER_FLAG = "TAP_EXPOSURE_NOTIFICATION_REMINDER"
    }

    private fun createAreaRiskChangedNotificationChannel() {
        createNotificationChannel(
            channelId = RISK_CHANGED_CHANNEL_ID,
            channelNameResId = R.string.notification_channel_area_risk_changed_name,
            importance = NotificationManagerCompat.IMPORTANCE_MAX,
            channelDescriptionResId = R.string.notification_channel_area_risk_changed_description
        )
    }

    private fun createIsolationStateNotificationChannel() {
        createNotificationChannel(
            channelId = ISOLATION_STATE_CHANNEL_ID,
            channelNameResId = R.string.notification_channel_isolation_state_name,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            channelDescriptionResId = R.string.notification_channel_isolation_state_description
        )
    }

    private fun createTestResultsNotificationChannel() {
        createNotificationChannel(
            channelId = TEST_RESULTS_CHANNEL_ID,
            channelNameResId = R.string.notification_channel_test_results_name,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            channelDescriptionResId = R.string.notification_channel_test_results_description
        )
    }

    private fun createAppAvailabilityNotificationChannel() {
        createNotificationChannel(
            channelId = APP_AVAILABILITY_CHANNEL_ID,
            channelNameResId = R.string.notification_channel_app_availability_name,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            channelDescriptionResId = R.string.notification_channel_app_availability_description
        )
    }

    private fun createAppConfigurationNotificationChannel() {
        createNotificationChannel(
            channelId = APP_CONFIGURATION_CHANNEL_ID,
            channelNameResId = R.string.notification_channel_app_configuration_name,
            importance = NotificationManagerCompat.IMPORTANCE_HIGH,
            channelDescriptionResId = R.string.notification_channel_app_configuration_description
        )
    }

    private fun createBackgroundWorkNotificationChannel() {
        createNotificationChannel(
            channelId = BACKGROUND_WORK_CHANNEL_ID,
            channelNameResId = R.string.notification_channel_background_work_name,
            importance = NotificationManagerCompat.IMPORTANCE_LOW,
            channelDescriptionResId = R.string.notification_channel_background_work_description
        )
    }

    fun showAreaRiskChangedNotification() {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_SHOW_AREA_RISK_CHANGED_NOTIFICATION,
                statusActivityIntent,
                0
            )

        val areaRiskChangedNotification = createNotification(
            RISK_CHANGED_CHANNEL_ID,
            R.string.notification_title_post_code_risk_changed,
            R.string.notification_text_post_code_risk_changed,
            pendingIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                AREA_RISK_CHANGED_NOTIFICATION_ID,
                areaRiskChangedNotification
            )
    }

    fun showRiskyVenueVisitNotification() {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_SHOW_RISKY_VENUE_VISIT_NOTIFICATION,
                statusActivityIntent,
                0
            )

        val riskyVenueNotification = createNotification(
            RISK_CHANGED_CHANNEL_ID,
            title = null,
            message = R.string.notification_title_risky_venue,
            contentIntent = pendingIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                RISKY_VENUE_VISIT_NOTIFICATION_ID,
                riskyVenueNotification
            )
    }

    fun showStateExpirationNotification() {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_SHOW_STATE_EXPIRATION_NOTIFICATION,
                statusActivityIntent,
                0
            )

        val expirationNotification = createNotification(
            ISOLATION_STATE_CHANNEL_ID,
            R.string.notification_title_state_expiration,
            R.string.notification_text_state_expiration,
            pendingIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                STATE_EXPIRATION_NOTIFICATION_ID,
                expirationNotification
            )
    }

    fun showExposureNotification() {
        val exposedNotificationActivity = EncounterDetectionActivity.getIntent(context)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_SHOW_EXPOSURE_NOTIFICATION,
                exposedNotificationActivity,
                0
            )

        val exposureNotification = createNotification(
            ISOLATION_STATE_CHANNEL_ID,
            title = null,
            message = getStringOrNull(R.string.notification_title_state_exposure) + "\n\n" + getStringOrNull(
                R.string.notification_text_state_exposure
            ),
            contentIntent = pendingIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                STATE_EXPOSURE_NOTIFICATION_ID,
                exposureNotification
            )
    }

    fun showExposureNotificationReminder() {
        val contentIntentActivity = Intent(context, StatusActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val contentIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_NOTIFICATION_REMINDER_CONTENT_INTENT,
                contentIntentActivity,
                0
            )

        val actionIntentActivity = Intent(context, StatusActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(
                TAP_EXPOSURE_NOTIFICATION_REMINDER_FLAG,
                TAP_EXPOSURE_NOTIFICATION_REMINDER_FLAG
            )
        }
        val actionIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_NOTIFICATION_REMINDER_ACTION_INTENT,
                actionIntentActivity,
                0
            )

        val exposureReminderNotification = createNotification(
            APP_CONFIGURATION_CHANNEL_ID,
            title = R.string.notification_title_exposure_reminder,
            message = null,
            contentIntent = contentIntent,
            actionText = R.string.notification_action_exposure_reminder,
            actionIntent = actionIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                EXPOSURE_REMINDER_NOTIFICATION_ID,
                exposureReminderNotification
            )
    }

    fun showTestResultsReceivedNotification() {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_SHOW_TEST_RESULTS,
                statusActivityIntent,
                0
            )

        val testResultsReceivedNotification = createNotification(
            TEST_RESULTS_CHANNEL_ID,
            R.string.notification_title_test_results,
            R.string.notification_text_test_results,
            pendingIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                TEST_RESULTS_NOTIFICATION_ID,
                testResultsReceivedNotification
            )
    }

    fun cancelTestResult() {
        NotificationManagerCompat.from(context)
            .cancel(
                TEST_RESULTS_NOTIFICATION_ID
            )
    }

    fun showAppIsAvailable() {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_APP_IS_AVAILABLE,
                statusActivityIntent,
                0
            )

        val appAvailableNotification = createNotification(
            APP_AVAILABILITY_CHANNEL_ID,
            R.string.notification_title_app_available,
            R.string.notification_text_app_availability,
            pendingIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                APP_AVAILABLE_NOTIFICATION_ID,
                appAvailableNotification
            )
    }

    fun showAppIsNotAvailable() {
        val appAvailabilityIntent = Intent(context, AppAvailabilityActivity::class.java)
        appAvailabilityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_APP_IS_NOT_AVAILABLE,
                appAvailabilityIntent,
                0
            )

        val appAvailableNotification = createNotification(
            APP_AVAILABILITY_CHANNEL_ID,
            R.string.notification_title_app_not_available,
            R.string.notification_text_app_availability,
            pendingIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                APP_NOT_AVAILABLE_NOTIFICATION_ID,
                appAvailableNotification
            )
    }

    fun showPotentialExposureExplanationNotification() {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_POTENTIAL_EXPOSURE_EXPLANATION_NOTIFICATION,
                statusActivityIntent,
                0
            )

        val notification = createNotification(
            ISOLATION_STATE_CHANNEL_ID,
            R.string.notification_title_potential_exposure_explanation,
            R.string.notification_text_potential_exposure_explanation,
            pendingIntent,
            useCategoryAlarm = false
        )

        NotificationManagerCompat.from(context)
            .notify(
                POTENTIAL_EXPOSURE_EXPLANATION_NOTIFICATION_ID,
                notification
            )
    }

    fun hidePotentialExposureExplanationNotification() {
        NotificationManagerCompat.from(context)
            .cancel(POTENTIAL_EXPOSURE_EXPLANATION_NOTIFICATION_ID)
    }

    fun getUpdatingDatabaseNotification(): Notification {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_CODE_UPDATING_DATABASE_NOTIFICATION,
                statusActivityIntent,
                0
            )

        return createNotification(
            BACKGROUND_WORK_CHANNEL_ID,
            R.string.app_name,
            R.string.notification_text_updating_database,
            pendingIntent,
            useCategoryAlarm = false
        )
    }

    fun canSendNotificationToChannel(channelId: String): Boolean {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val manager =
                context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(channelId)
            if (channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun createNotificationChannel(
        channelId: String,
        @StringRes channelNameResId: Int,
        importance: Int,
        @StringRes channelDescriptionResId: Int
    ) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, context.getString(channelNameResId), importance
            ).apply {
                description = context.getString(channelDescriptionResId)
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        notificationChannel: String,
        @StringRes title: Int?,
        @StringRes message: Int?,
        contentIntent: PendingIntent,
        @StringRes actionText: Int? = null,
        actionIntent: PendingIntent? = null,
        autoCancel: Boolean = true,
        useCategoryAlarm: Boolean = true
    ): Notification {
        return createNotification(
            notificationChannel,
            getStringOrNull(title),
            getStringOrNull(message),
            contentIntent,
            actionText,
            actionIntent,
            autoCancel,
            useCategoryAlarm
        )
    }

    private fun getStringOrNull(@StringRes stringId: Int?) =
        if (stringId != null) context.getString(stringId) else null

    private fun createNotification(
        notificationChannel: String,
        title: String?,
        message: String?,
        contentIntent: PendingIntent,
        @StringRes actionText: Int? = null,
        actionIntent: PendingIntent? = null,
        autoCancel: Boolean = true,
        useCategoryAlarm: Boolean = true
    ) =
        NotificationCompat.Builder(context, notificationChannel)
            .setSmallIcon(R.mipmap.ic_notification)
            .apply {
                if (useCategoryAlarm) {
                    setCategory(NotificationCompat.CATEGORY_ALARM) // Shows notification in DND mode
                }
            }
            .apply {
                if (title != null) {
                    setContentTitle(title)
                }
            }
            .apply {
                if (message != null) {
                    setContentText(message)
                    setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(message)
                    )
                }
            }
            .apply {
                actionText?.let {
                    addAction(
                        0,
                        context.getString(it),
                        actionIntent ?: contentIntent
                    )
                }
            }
            .setAutoCancel(autoCancel)
            .setContentIntent(contentIntent)
            .build()
}
