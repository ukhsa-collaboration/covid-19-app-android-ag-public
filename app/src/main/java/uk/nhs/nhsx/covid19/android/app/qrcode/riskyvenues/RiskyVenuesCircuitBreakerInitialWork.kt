package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.work.ListenableWorker.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerRequest
import javax.inject.Inject

class RiskyVenuesCircuitBreakerInitialWork @Inject constructor(
    private val riskyVenuesCircuitBreakerApi: RiskyVenuesCircuitBreakerApi,
    private val notificationProvider: NotificationProvider,
    private val periodicTasks: PeriodicTasks,
    private val userInbox: UserInbox
) {

    suspend fun doWork(venueId: String): Result = withContext(Dispatchers.IO) {
        try {
            val response = riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(
                RiskyVenuesCircuitBreakerRequest(venueId)
            )

            return@withContext when (response.approval) {
                YES -> {
                    notifyUser(venueId)
                    Result.success()
                }
                NO -> Result.success()
                PENDING -> {
                    startPolling(response.approvalToken, venueId)
                    Result.success()
                }
            }
        } catch (e: Exception) {
            return@withContext Result.retry()
        }
    }

    private fun startPolling(approvalToken: String, venueId: String) {
        periodicTasks.scheduleRiskyVenuesCircuitBreakerPolling(approvalToken, venueId)
    }

    private fun notifyUser(venueId: String) {
        userInbox.addUserInboxItem(ShowVenueAlert(venueId))
        notificationProvider.showRiskyVenueVisitNotification()
    }
}
