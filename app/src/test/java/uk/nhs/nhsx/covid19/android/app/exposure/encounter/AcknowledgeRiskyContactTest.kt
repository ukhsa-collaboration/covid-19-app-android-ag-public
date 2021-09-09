package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.ScheduleIsolationHubReminder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AcknowledgeRiskyContactTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val shouldShowEncounterDetectionActivityProvider =
        mockk<ShouldShowEncounterDetectionActivityProvider>(relaxUnitFun = true)
    private val exposureNotificationRetryAlarmController =
        mockk<ExposureNotificationRetryAlarmController>(relaxed = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val scheduleIsolationHubReminder = mockk<ScheduleIsolationHubReminder>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationLogicalHelper(fixedClock)

    private val acknowledgeRiskyContact = AcknowledgeRiskyContact(
        exposureNotificationRetryAlarmController,
        shouldShowEncounterDetectionActivityProvider,
        analyticsEventProcessor,
        isolationStateMachine,
        scheduleIsolationHubReminder,
        fixedClock
    )

    @Test
    fun `when acknowledging contact case isolation and already isolating for different reason then do not schedule isolation hub reminder notification`() {
        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.selfAssessment().asIsolation()

        acknowledgeRiskyContact()

        verify { exposureNotificationRetryAlarmController.cancel() }
        verify { shouldShowEncounterDetectionActivityProvider setProperty "value" value null }
        verify { analyticsEventProcessor.track(AcknowledgedStartOfIsolationDueToRiskyContact) }
        verify(exactly = 0) { scheduleIsolationHubReminder() }
    }

    @Test
    fun `when acknowledging contact case isolation and currently only isolating due to contact case then schedule isolation hub reminder notification`() {
        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.contactCase().asIsolation()

        acknowledgeRiskyContact()

        verify { exposureNotificationRetryAlarmController.cancel() }
        verify { shouldShowEncounterDetectionActivityProvider setProperty "value" value null }
        verify { analyticsEventProcessor.track(AcknowledgedStartOfIsolationDueToRiskyContact) }
        verify { scheduleIsolationHubReminder() }
    }
}
