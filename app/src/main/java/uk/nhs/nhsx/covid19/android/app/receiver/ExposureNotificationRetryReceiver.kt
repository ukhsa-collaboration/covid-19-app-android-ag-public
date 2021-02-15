package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController

class ExposureNotificationRetryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        exposureNotificationRetryAlarmController.onAlarmTriggered()
    }
}
