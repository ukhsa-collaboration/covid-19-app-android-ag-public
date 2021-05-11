package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.Intent.ACTION_MY_PACKAGE_REPLACED
import com.jeroenmols.featureflag.framework.FeatureFlag.SUBMIT_ANALYTICS_VIA_ALARM_MANAGER
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalyticsAlarmController
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationAlarmController
import uk.nhs.nhsx.covid19.android.app.status.ResumeContactTracingNotificationTimeProvider
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
    lateinit var resumeContactTracingNotificationTimeProvider: ResumeContactTracingNotificationTimeProvider

    @Inject
    lateinit var exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        val action = intent.action
        if (action != ACTION_BOOT_COMPLETED && action != ACTION_MY_PACKAGE_REPLACED) return

        exposureNotificationRetryAlarmController.onDeviceRebooted()
        if (RuntimeBehavior.isFeatureEnabled(SUBMIT_ANALYTICS_VIA_ALARM_MANAGER)) {
            submitAnalyticsAlarmController.onDeviceRebooted()
        }

        isolationExpirationAlarmController.onDeviceRebooted()

        resumeContactTracingNotificationTimeProvider.value?.let {
            val alarmTime = Instant.ofEpochMilli(it)
            exposureNotificationReminderAlarmController.setup(alarmTime)
        }
    }
}
