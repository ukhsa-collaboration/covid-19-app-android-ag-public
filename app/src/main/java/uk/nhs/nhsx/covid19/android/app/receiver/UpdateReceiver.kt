package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.analytics.UpdateStatusStorage
import uk.nhs.nhsx.covid19.android.app.appComponent
import javax.inject.Inject

class UpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var updateStatusStorage: UpdateStatusStorage

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
            return

        updateStatusStorage.value = true
    }
}
