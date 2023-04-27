package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.Intent.ACTION_MY_PACKAGE_REPLACED
import androidx.work.WorkManager
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import com.jeroenmols.featureflag.framework.FeatureFlag.SUBMIT_ANALYTICS_VIA_ALARM_MANAGER
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalyticsAlarmController
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTask.PERIODIC_TASKS
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.settings.DeleteAllUserData
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationAlarmController
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingActivationReminderProvider
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.MigrateContactTracingActivationReminderProvider
import java.time.Instant
import javax.inject.Inject

class AlarmRestarter : BroadcastReceiver() {

    @Inject
    lateinit var isolationExpirationAlarmController: IsolationExpirationAlarmController

    @Inject
    lateinit var exposureNotificationReminderAlarmController: ExposureNotificationReminderAlarmController

    @Inject
    lateinit var submitAnalyticsAlarmController: SubmitAnalyticsAlarmController

    @Inject
    lateinit var contactTracingActivationReminderProvider: ContactTracingActivationReminderProvider

    @Inject
    lateinit var migrateContactTracingActivationReminderProvider: MigrateContactTracingActivationReminderProvider

    @Inject
    lateinit var exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController

    @Inject
    lateinit var deleteAllUserData: DeleteAllUserData

    @Inject
    lateinit var notificationProvider: NotificationProvider

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        val action = intent.action
        if (action != ACTION_BOOT_COMPLETED && action != ACTION_MY_PACKAGE_REPLACED) return

        if (RuntimeBehavior.isFeatureEnabled(DECOMMISSIONING_CLOSURE_SCREEN)) {
            if (action == ACTION_MY_PACKAGE_REPLACED) {
                Timber.d("App updated in decommissioning state")

                notificationProvider.showAppHasBeenDecommissionedNotification()

                deleteAllUserData(shouldKeepLanguage = true)

                val workManager = WorkManager.getInstance(context)
                workManager.cancelUniqueWork(PERIODIC_TASKS.workName)
                workManager.cancelUniqueWork("SubmitAnalyticsWorkerOnboardingFinished")

                submitAnalyticsAlarmController.cancelIfScheduled()
            } else {
                return
            }
        } else {

            exposureNotificationRetryAlarmController.onDeviceRebooted()

            if (RuntimeBehavior.isFeatureEnabled(SUBMIT_ANALYTICS_VIA_ALARM_MANAGER)) {
                submitAnalyticsAlarmController.onDeviceRebooted()
            }

            isolationExpirationAlarmController.onDeviceRebooted()

            migrateContactTracingActivationReminderProvider()

            contactTracingActivationReminderProvider.reminder?.let {
                val alarmTime = Instant.ofEpochMilli(it.alarmTime)
                exposureNotificationReminderAlarmController.setup(alarmTime)
            }
        }
    }
}
