package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM1Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM2Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.NO
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.PENDING
import uk.nhs.nhsx.covid19.android.app.common.CircuitBreakerResult.YES
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlert
import uk.nhs.nhsx.covid19.android.app.notifications.RiskyVenueAlertProvider
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenuesCircuitBreakerApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.BOOK_TEST
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import javax.inject.Inject

class RiskyVenuesCircuitBreakerPolling @Inject constructor(
    private val riskyVenuesCircuitBreakerApi: RiskyVenuesCircuitBreakerApi,
    private val notificationProvider: NotificationProvider,
    private val riskyVenueAlertProvider: RiskyVenueAlertProvider,
    private val riskyVenueCircuitBreakerConfigurationProvider: RiskyVenueCircuitBreakerConfigurationProvider,
    private val removeOutdatedRiskyVenuePollingConfigurations: RemoveOutdatedRiskyVenuePollingConfigurations,
    private val lastVisitedBookTestTypeVenueDateProvider: LastVisitedBookTestTypeVenueDateProvider,
    private val shouldShowRiskyVenueNotification: ShouldShowRiskyVenueNotification,
    private val riskyVenueConfigurationProvider: RiskyVenueConfigurationProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val clock: Clock
) {

    suspend operator fun invoke() {
        removeOutdatedRiskyVenuePollingConfigurations.invoke()

        var latestApprovedConfig: RiskyVenueCircuitBreakerConfiguration? = null

        riskyVenueCircuitBreakerConfigurationProvider.configs.forEach { config ->
            runCatching {
                val (approval, approvalToken) = if (config.isPolling) {
                    Pair(
                        riskyVenuesCircuitBreakerApi.getRiskyVenuesBreakerResolution(config.approvalToken!!).approval,
                        config.approvalToken
                    )
                } else {
                    val response = riskyVenuesCircuitBreakerApi.getApproval()
                    Pair(response.approval, response.approvalToken)
                }

                when (approval) {
                    YES -> {
                        val existingApprovedConfig = latestApprovedConfig
                        if (existingApprovedConfig?.messageType == BOOK_TEST) {
                            if (config.messageType == BOOK_TEST && existingApprovedConfig.startedAt.isBefore(config.startedAt)) {
                                latestApprovedConfig = config
                            }
                        } else {
                            latestApprovedConfig = config
                        }
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
            if (it.messageType == BOOK_TEST) {
                lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = LastVisitedBookTestTypeVenueDate(
                    it.startedAt.toLocalDate(clock.zone),
                    riskyVenueConfigurationProvider.durationDays
                )
                analyticsEventProcessor.track(ReceivedRiskyVenueM2Warning)
            } else {
                analyticsEventProcessor.track(ReceivedRiskyVenueM1Warning)
            }
            if (shouldShowRiskyVenueNotification(it.messageType)) {
                notificationProvider.showRiskyVenueVisitNotification(messageType = it.messageType)
            }
            riskyVenueAlertProvider.riskyVenueAlert = RiskyVenueAlert(it.venueId, it.messageType)
        }
    }
}
