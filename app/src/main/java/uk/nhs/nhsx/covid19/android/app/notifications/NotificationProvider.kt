package uk.nhs.nhsx.covid19.android.app.notifications

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.StringRes
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
    }

    companion object {
        const val RISK_CHANGED_CHANNEL_ID = "AREA_RISK_CHANGED"
        const val ISOLATION_STATE_CHANNEL_ID = "ISOLATION_STATE"
        const val TEST_RESULTS_CHANNEL_ID = "TEST_RESULTS"
        const val APP_AVAILABILITY_CHANNEL_ID = "APP_AVAILABILITY"
        const val AREA_RISK_CHANGED_NOTIFICATION_ID = 0
        const val RISKY_VENUE_VISIT_NOTIFICATION_ID = 1
        const val STATE_EXPIRATION_NOTIFICATION_ID = 2
        const val STATE_EXPOSURE_NOTIFICATION_ID = 3
        const val TEST_RESULTS_NOTIFICATION_ID = 4
        const val APP_AVAILABLE_NOTIFICATION_ID = 5
        const val APP_NOT_AVAILABLE_NOTIFICATION_ID = 6
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

    fun showAreaRiskChangedNotification() {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, statusActivityIntent, 0)

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
            PendingIntent.getActivity(context, 0, statusActivityIntent, 0)

        val riskyVenueNotification = createNotification(
            RISK_CHANGED_CHANNEL_ID,
            R.string.notification_title_risky_venue,
            R.string.notification_text_risky_venue,
            pendingIntent
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
            PendingIntent.getActivity(context, 1, statusActivityIntent, 0)

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
            PendingIntent.getActivity(context, 0, exposedNotificationActivity, 0)

        val exposureNotification = createNotification(
            ISOLATION_STATE_CHANNEL_ID,
            R.string.notification_title_state_exposure,
            R.string.notification_text_state_exposure,
            pendingIntent
        )

        NotificationManagerCompat.from(context)
            .notify(
                STATE_EXPOSURE_NOTIFICATION_ID,
                exposureNotification
            )
    }

    fun showTestResultsReceivedNotification() {
        val statusActivityIntent = Intent(context, StatusActivity::class.java)
        statusActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, statusActivityIntent, 0)

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
            PendingIntent.getActivity(context, 0, statusActivityIntent, 0)

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
            PendingIntent.getActivity(context, 0, appAvailabilityIntent, 0)

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

    private fun createNotificationChannel(
        channelId: String,
        @StringRes channelNameResId: Int,
        importance: Int,
        @StringRes channelDescriptionResId: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        @StringRes message: Int,
        @StringRes actionText: Int?,
        pendingIntent: PendingIntent,
        autoCancel: Boolean = true
    ) =
        NotificationCompat.Builder(context, notificationChannel)
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentText(context.getString(message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(message)))
            .apply {
                actionText?.let {
                    addAction(0, context.getString(it), pendingIntent)
                }
            }
            .setAutoCancel(autoCancel)
            .setContentIntent(pendingIntent)
            .build()
}
