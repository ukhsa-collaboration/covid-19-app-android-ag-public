package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import javax.inject.Inject

class ExposureNotificationBroadcastReceiver : BroadcastReceiver() {

    private val allowedActions = listOf(
        ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED,
        ExposureNotificationClient.ACTION_EXPOSURE_NOT_FOUND
    )

    @Inject
    lateinit var exposureNotificationWorkerScheduler: ExposureNotificationWorkerScheduler

    @Inject
    lateinit var isolationStateMachine: IsolationStateMachine

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        val action = intent.action
        Timber.d("onReceive: action = $action")
        if (action in allowedActions) {
            if (isolationStateMachine.isInterestedInExposureNotifications()) {
                exposureNotificationWorkerScheduler.scheduleEvaluateRisk(context)
            } else {
                exposureNotificationWorkerScheduler.scheduleDoNotEvaluateRisk(context)
            }
        }
    }
}
