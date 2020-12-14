package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.appComponent
import javax.inject.Inject

class ExposureNotificationBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var exposureNotificationsTokensProvider: ExposureNotificationTokensProvider

    @Inject
    lateinit var exposureNotificationWorkerScheduler: ExposureNotificationWorkerScheduler

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)

        val action = intent.action
        Timber.d("onReceive: action = $action")
        val token = intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN) ?: emptyToken
        Timber.d("onReceive: token = $token")
        if (ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED == action) {
            exposureNotificationsTokensProvider.add(token)
            exposureNotificationWorkerScheduler.scheduleMatchesFound(context, token)
        } else if (ExposureNotificationClient.ACTION_EXPOSURE_NOT_FOUND == action) {
            exposureNotificationWorkerScheduler.scheduleNoMatchesFound(context)
        }
    }

    companion object {
        private const val emptyToken = "empty"
    }
}
