package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import androidx.work.ListenableWorker.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenuesCircuitBreakerResponse
import java.time.Instant
import javax.inject.Inject

class RiskyVenuesCircuitBreakerInitialWork @Inject constructor(
    private val riskyVenuesCircuitBreakerApi: RiskyVenuesCircuitBreakerApi,
    private val notificationProvider: NotificationProvider,
    private val userInbox: UserInbox,
    private val riskyVenuePollingConfigurationProvider: RiskyVenuePollingConfigurationProvider,
    private val visitedVenuesStorage: VisitedVenuesStorage
) {

    suspend fun doWork(riskyVenueIds: List<String>): Result =
        withContext(Dispatchers.IO) {
            riskyVenueIds.forEach { riskyVenueId ->
                try {
                    val response: RiskyVenuesCircuitBreakerResponse =
                        riskyVenuesCircuitBreakerApi.submitInitialVenueIdForApproval(
                            RiskyVenuesCircuitBreakerRequest(riskyVenueId)
                        )

                    when (response.approval) {
                        YES -> notifyUser(riskyVenueId)
                        PENDING -> {
                            riskyVenuePollingConfigurationProvider.add(
                                RiskyVenuePollingConfiguration(
                                    startedAt = Instant.now(),
                                    venueId = riskyVenueId,
                                    approvalToken = response.approvalToken
                                )
                            )
                        }
                        else -> Unit
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    visitedVenuesStorage.undoMarkWasInRiskyList(riskyVenueId)
                }
            }
            return@withContext Result.success()
        }

    private fun notifyUser(venueId: String) {
        userInbox.addUserInboxItem(ShowVenueAlert(venueId))
        notificationProvider.showRiskyVenueVisitNotification()
    }
}
