package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationAlarmController
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import javax.inject.Inject

class AlarmRestarter : BroadcastReceiver() {

    @Inject
    lateinit var isolationStateMachine: IsolationStateMachine
    @Inject
    lateinit var isolationExpirationAlarmController: IsolationExpirationAlarmController

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)
        if (intent.action == "android.intent.action.BOOT_COMPLETED" || intent.action == "android.intent.action.MY_PACKAGE_REPLACED") {
            val expiryDate = when (val state = isolationStateMachine.readState()) {
                is Isolation -> state.expiryDate
                is Default -> return
            }
            isolationExpirationAlarmController.setupExpirationCheck(expiryDate)
        }
    }
}
