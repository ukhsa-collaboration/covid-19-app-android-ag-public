package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.state.DisplayStateExpirationNotification
import javax.inject.Inject

class ExpirationCheckReceiver : BroadcastReceiver() {

    @Inject
    lateinit var displayStateExpirationNotification: DisplayStateExpirationNotification

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)
        displayStateExpirationNotification.doWork()
    }
}
