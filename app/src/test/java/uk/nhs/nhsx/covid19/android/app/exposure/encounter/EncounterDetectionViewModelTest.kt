package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EncounterDetectionViewModel.ExposedNotificationResult
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationRetryAlarmController
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.ShouldShowEncounterDetectionActivityProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class EncounterDetectionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val shouldShowEncounterDetectionActivityProvider =
        mockk<ShouldShowEncounterDetectionActivityProvider>(relaxUnitFun = true)
    private val exposureNotificationRetryAlarmController =
        mockk<ExposureNotificationRetryAlarmController>(relaxed = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val resultObserver = mockk<Observer<ExposedNotificationResult>>(
        relaxed = true
    )

    private val testSubject = EncounterDetectionViewModel(
        isolationStateMachine,
        shouldShowEncounterDetectionActivityProvider,
        exposureNotificationRetryAlarmController,
        analyticsEventProcessor,
        fixedClock
    )

    @Before
    fun setUp() {
        testSubject.isolationState().observeForever(resultObserver)
    }

    @Test
    fun `provide the days of isolation left when in contact case`() {
        every { isolationStateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    exposureDate = LocalDate.now(fixedClock),
                    notificationDate = LocalDate.now(fixedClock),
                    expiryDate = LocalDate.now(fixedClock).plusDays(14)
                )
            ).asLogical()
        every { isolationStateMachine.remainingDaysInIsolation(any()) } returns 14

        testSubject.getIsolationDays()

        verify { resultObserver.onChanged(ExposedNotificationResult.IsolationDurationDays(14)) }
    }

    @Test
    fun `confirm consent`() = runBlocking {
        testSubject.confirmConsent()

        verify { exposureNotificationRetryAlarmController.cancel() }
        verify { shouldShowEncounterDetectionActivityProvider setProperty "value" value null }
        verify { analyticsEventProcessor.track(AcknowledgedStartOfIsolationDueToRiskyContact) }

        verify { resultObserver.onChanged(ExposedNotificationResult.ConsentConfirmation) }
    }

    @Test
    fun `state is Default`() {
        every { isolationStateMachine.readLogicalState() } returns
            IsolationState(isolationConfiguration = DurationDays()).asLogical()

        testSubject.getIsolationDays()

        verify(exactly = 0) { resultObserver.onChanged(any()) }
    }
}
