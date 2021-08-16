package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import javax.inject.Inject

class IsolationHubReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationProvider: NotificationProvider

    @Inject
    lateinit var isolationHubReminderTimeProvider: IsolationHubReminderTimeProvider

    @Inject
    lateinit var isolationStateMachine: IsolationStateMachine

    @Inject
    lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        context.appComponent.inject(this)
        if (isolationStateMachine.readLogicalState().isActiveIsolation(clock)) {
            notificationProvider.showIsolationHubReminderNotification()
        }
        isolationHubReminderTimeProvider.value = null
    }
}
