package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ScheduleContactTracingActivationAdditionalReminderIfNeeded
import javax.inject.Inject

class ExposureNotificationReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationProvider: NotificationProvider

    @Inject
    lateinit var scheduleContactTracingActivationAdditionalReminderIfNeeded: ScheduleContactTracingActivationAdditionalReminderIfNeeded

    @Inject
    lateinit var exposureNotificationApi: ExposureNotificationApi

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        if (RuntimeBehavior.isFeatureEnabled(DECOMMISSIONING_CLOSURE_SCREEN)) {
            return
        }

        CoroutineScope(SupervisorJob()).launch {
            if (!exposureNotificationApi.isEnabled()) {
                notificationProvider.showExposureNotificationReminder()
                scheduleContactTracingActivationAdditionalReminderIfNeeded()
            }
        }
    }
}
