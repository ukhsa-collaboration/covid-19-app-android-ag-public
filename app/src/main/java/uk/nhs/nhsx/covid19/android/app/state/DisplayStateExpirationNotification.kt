package uk.nhs.nhsx.covid19.android.app.state

import androidx.work.ListenableWorker.Result
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.Notify
import javax.inject.Inject

class DisplayStateExpirationNotification @Inject constructor(
    private val shouldNotifyStateExpiration: ShouldNotifyStateExpiration,
    private val notificationProvider: NotificationProvider,
    private val isolationExpirationAlarmProvider: IsolationExpirationAlarmProvider
) {
    fun doWork(): Result {
        Timber.d("doWork")

        isolationExpirationAlarmProvider.value = null

        if (shouldNotifyStateExpiration() is Notify) {
            notificationProvider.showStateExpirationNotification()
        }

        return Result.success()
    }
}
