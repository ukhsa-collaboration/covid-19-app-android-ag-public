package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsAlarm
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationAlarmController
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.status.ResumeContactTracingNotificationTimeProvider
import java.time.Instant
import javax.inject.Inject

class AlarmRestarter : BroadcastReceiver() {

    @Inject
    lateinit var isolationStateMachine: IsolationStateMachine

    @Inject
    lateinit var isolationExpirationAlarmController: IsolationExpirationAlarmController

    @Inject
    lateinit var analyticsAlarm: AnalyticsAlarm

    @Inject
    lateinit var exposureNotificationReminderAlarmController: ExposureNotificationReminderAlarmController

    @Inject
    lateinit var resumeContactTracingNotificationTimeProvider: ResumeContactTracingNotificationTimeProvider

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val expiryDate = when (val state = isolationStateMachine.readState()) {
                is Isolation -> state.expiryDate
                is Default -> return
            }
            isolationExpirationAlarmController.setupExpirationCheck(expiryDate)

            resumeContactTracingNotificationTimeProvider.value?.let {
                val alarmTime = Instant.ofEpochMilli(it)
                exposureNotificationReminderAlarmController.setup(alarmTime)
            }

            analyticsAlarm.scheduleNextAnalyticsAggregator()
        }
    }
}
