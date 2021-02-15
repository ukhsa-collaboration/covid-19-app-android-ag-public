package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.Intent.ACTION_MY_PACKAGE_REPLACED
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
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
    lateinit var exposureNotificationReminderAlarmController: ExposureNotificationReminderAlarmController

    @Inject
    lateinit var resumeContactTracingNotificationTimeProvider: ResumeContactTracingNotificationTimeProvider

    @Inject
    lateinit var exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        val action = intent.action
        if (action != ACTION_BOOT_COMPLETED && action != ACTION_MY_PACKAGE_REPLACED) return

        exposureNotificationRetryAlarmController.onDeviceRebooted()

        val expiryDate = when (val state = isolationStateMachine.readState()) {
            is Isolation -> state.expiryDate
            is Default -> return
        }
        isolationExpirationAlarmController.setupExpirationCheck(expiryDate)

        resumeContactTracingNotificationTimeProvider.value?.let {
            val alarmTime = Instant.ofEpochMilli(it)
            exposureNotificationReminderAlarmController.setup(alarmTime)
        }
    }
}
