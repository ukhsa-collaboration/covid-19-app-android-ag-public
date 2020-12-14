package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import javax.inject.Inject

class RiskyVenuesCircuitBreakerPolling @Inject constructor(
    private val riskyVenuesCircuitBreakerApi: RiskyVenuesCircuitBreakerApi,
    private val notificationProvider: NotificationProvider,
    private val userInbox: UserInbox,
    private val riskyVenueCircuitBreakerConfigurationProvider: RiskyVenueCircuitBreakerConfigurationProvider,
    private val removeOutdatedRiskyVenuePollingConfigurations: RemoveOutdatedRiskyVenuePollingConfigurations
) {

    suspend operator fun invoke() {
        removeOutdatedRiskyVenuePollingConfigurations.invoke()

        var latestApprovedConfig: RiskyVenueCircuitBreakerConfiguration? = null

        riskyVenueCircuitBreakerConfigurationProvider.configs.forEach { config ->
            runCatching {
                val (approval, approvalToken) = if (config.isPolling) {
                    Pair(riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(config.approvalToken!!).approval, config.approvalToken)
                } else {
                    val response = riskyVenuesCircuitBreakerApi.getApproval()
                    Pair(response.approval, response.approvalToken)
                }

                when (approval) {
                    YES -> {
                        latestApprovedConfig = config
                        riskyVenueCircuitBreakerConfigurationProvider.remove(config)
                    }
                    NO -> riskyVenueCircuitBreakerConfigurationProvider.remove(config)
                    PENDING -> {
                        if (!config.isPolling) {
                            val pollingConfig = config.copy(isPolling = true, approvalToken = approvalToken)
                            riskyVenueCircuitBreakerConfigurationProvider.remove(config)
                            riskyVenueCircuitBreakerConfigurationProvider.add(pollingConfig)
                        }
                    }
                }
            }
        }
        latestApprovedConfig?.let {
            notificationProvider.showRiskyVenueVisitNotification()
            userInbox.addUserInboxItem(ShowVenueAlert(it.venueId))
        }
    }
}
