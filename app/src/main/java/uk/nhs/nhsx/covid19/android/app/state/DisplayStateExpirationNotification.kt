package uk.nhs.nhsx.covid19.android.app.state

import androidx.work.ListenableWorker.Result
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowIsolationExpiration
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class DisplayStateExpirationNotification(
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock,
    private val notificationProvider: NotificationProvider,
    private val userInbox: UserInbox
) {
    @Inject
    constructor(
        isolationStateMachine: IsolationStateMachine,
        notificationProvider: NotificationProvider,
        userInbox: UserInbox
    ) : this(isolationStateMachine, Clock.systemDefaultZone(), notificationProvider, userInbox)

    fun doWork(): Result {
        Timber.d("doWork")
        when (val currentState = isolationStateMachine.readState(validateExpiry = false)) {
            is Default -> Result.success()
            is Isolation -> conditionallySendNotification(currentState.expiryDate)
        }

        return Result.success()
    }

    private fun conditionallySendNotification(expiryDate: LocalDate) {
        val tomorrow = isTomorrow(expiryDate)
        Timber.d("isTomorrow: $tomorrow $expiryDate")
        val isPast = isPast(expiryDate)
        Timber.d("inPast: $isPast $expiryDate")

        if (tomorrow || isPast) {
            notificationProvider.showStateExpirationNotification()
            userInbox.addUserInboxItem(ShowIsolationExpiration(expiryDate))
        }
    }

    private fun isTomorrow(expiryDate: LocalDate): Boolean {
        val today = LocalDate.now(clock)
        return today.plusDays(1) == expiryDate
    }

    private fun isPast(expiryDate: LocalDate): Boolean {
        val today = LocalDate.now(clock)
        return !expiryDate.isAfter(today)
    }
}
