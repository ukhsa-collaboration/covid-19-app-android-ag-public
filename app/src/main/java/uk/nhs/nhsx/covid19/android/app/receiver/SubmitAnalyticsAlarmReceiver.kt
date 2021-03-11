package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitAnalyticsAlarmController
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.appComponent

class SubmitAnalyticsAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var submitAnalyticsAlarmController: SubmitAnalyticsAlarmController

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        submitAnalyticsAlarmController.onAlarmTriggered()
    }
}
