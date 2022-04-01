package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider
import javax.inject.Inject

class AcknowledgeRiskyContact @Inject constructor(
    private val exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController,
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor
    ) {

    operator fun invoke() {
        exposureNotificationRetryAlarmController.cancel()
        shouldShowEncounterDetectionActivityProvider.value = null
        analyticsEventProcessor.track(AcknowledgedStartOfIsolationDueToRiskyContact)
    }
}
