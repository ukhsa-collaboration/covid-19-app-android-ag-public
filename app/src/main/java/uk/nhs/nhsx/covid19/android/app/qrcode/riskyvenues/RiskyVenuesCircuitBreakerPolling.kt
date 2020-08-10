package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.work.ListenableWorker.Result
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.notifications.AndroidUserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import javax.inject.Inject

class RiskyVenuesCircuitBreakerPolling @Inject constructor(
    private val riskyVenuesCircuitBreakerApi: RiskyVenuesCircuitBreakerApi,
    private val notificationProvider: NotificationProvider,
    private val userInbox: AndroidUserInbox
) {

    suspend fun doWork(approvalToken: String, venueId: String): Result =
        runCatching {
            val response =
                riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(approvalToken)

            when (response.approval) {
                YES -> {
                    notificationProvider.showRiskyVenueVisitNotification()
                    userInbox.addUserInboxItem(ShowVenueAlert(venueId))
                    Result.success()
                }
                NO -> Result.success()
                PENDING -> Result.retry()
            }
        }
            .getOrElse {
                Result.retry()
            }
}
