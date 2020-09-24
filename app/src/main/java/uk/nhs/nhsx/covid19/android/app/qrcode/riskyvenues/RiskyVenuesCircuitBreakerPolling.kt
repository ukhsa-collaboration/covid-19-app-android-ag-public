package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
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
    private val riskyVenuePollingConfigurationProvider: RiskyVenuePollingConfigurationProvider,
    private val removeOutdatedRiskyVenuePollingConfigurations: RemoveOutdatedRiskyVenuePollingConfigurations
) {

    suspend fun doWork() {
        if (!RuntimeBehavior.isFeatureEnabled(FeatureFlag.HIGH_RISK_VENUES)) {
            return
        }

        removeOutdatedRiskyVenuePollingConfigurations.invoke()

        var latestApprovedConfig: RiskyVenuePollingConfiguration? = null

        riskyVenuePollingConfigurationProvider.configs.forEach { config ->
            runCatching {
                val response =
                    riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(config.approvalToken)

                when (response.approval) {
                    YES -> {
                        latestApprovedConfig = config
                        riskyVenuePollingConfigurationProvider.remove(config)
                    }
                    NO -> riskyVenuePollingConfigurationProvider.remove(config)
                    else -> Unit
                }
            }
        }
        latestApprovedConfig?.let {
            notificationProvider.showRiskyVenueVisitNotification()
            userInbox.addUserInboxItem(ShowVenueAlert(it.venueId))
        }
    }
}
