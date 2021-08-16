package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.ScheduleIsolationHubReminder
import java.time.Clock
import javax.inject.Inject

class AcknowledgeRiskyContact @Inject constructor(
    private val exposureNotificationRetryAlarmController: ExposureNotificationRetryAlarmController,
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val isolationStateMachine: IsolationStateMachine,
    private val scheduleIsolationHubReminder: ScheduleIsolationHubReminder,
    private val clock: Clock,
    ) {

    operator fun invoke() {
        exposureNotificationRetryAlarmController.cancel()
        shouldShowEncounterDetectionActivityProvider.value = null
        analyticsEventProcessor.track(AcknowledgedStartOfIsolationDueToRiskyContact)

        /*
        When acknowledging the contact case isolation here, the isolation has already started (right after receiving the
        exposure notification). Since transitioning into contact case isolation twice is not possible, we check if the
        user started isolation as a contact case only and schedule the isolation hub reminder as a result.
         */
        if (isolationStateMachine.readLogicalState().isActiveContactCaseOnly(clock)) {
            scheduleIsolationHubReminder()
        }
    }
}
