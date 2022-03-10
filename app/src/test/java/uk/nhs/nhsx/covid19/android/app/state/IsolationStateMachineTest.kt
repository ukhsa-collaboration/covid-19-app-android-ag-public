package uk.nhs.nhsx.covid19.android.app.state

import com.tinder.StateMachine.Transition
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.isolation.createIsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.userinbox.StorageBasedUserInbox
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.QUESTIONNAIRE
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.IsolationHubReminderAlarmController
import uk.nhs.nhsx.covid19.android.app.status.isolationhub.ScheduleIsolationHubReminder
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class IsolationStateMachineTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val stateProvider = mockk<StateStorage>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val storageBasedUserInbox = mockk<StorageBasedUserInbox>(relaxUnitFun = true)
    private val alarmController = mockk<IsolationExpirationAlarmController>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxed = true)
    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxed = true)
    private val testResultIsolationHandler = mockk<TestResultIsolationHandler>(relaxed = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val exposureNotificationHandler = mockk<ExposureNotificationHandler>(relaxUnitFun = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val trackTestResultAnalyticsOnReceive = mockk<TrackTestResultAnalyticsOnReceive>(relaxUnitFun = true)
    private val trackTestResultAnalyticsOnAcknowledge =
        mockk<TrackTestResultAnalyticsOnAcknowledge>(relaxUnitFun = true)
    private val scheduleIsolationHubReminder = mockk<ScheduleIsolationHubReminder>(relaxUnitFun = true)
    private val isolationHubReminderAlarmController = mockk<IsolationHubReminderAlarmController>(relaxUnitFun = true)
    private val createIsolationState = mockk<CreateIsolationState>(relaxUnitFun = true)
    private val createIsolationLogicalState = createIsolationLogicalState(fixedClock)

    private val isolationConfiguration = IsolationConfiguration()
    private val durationDays = DurationDays()

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
        val slot = slot<IsolationInfo>()
        every { createIsolationState(capture(slot)) } answers {
            IsolationState(
                isolationConfiguration = IsolationConfiguration(),
                selfAssessment = slot.captured.selfAssessment,
                testResult = slot.captured.testResult,
                contact = slot.captured.contact,
                hasAcknowledgedEndOfIsolation = slot.captured.hasAcknowledgedEndOfIsolation
            )
        }
    }

    @Test
    fun `from default to index case on positive self-assessment and user cannot remember onset date`() {
        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(
                CannotRememberDate
            )
        )

        val actualState = testSubject.readState()
        val expectedState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        verify {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actualState
        )
    }

    @Test
    fun `stay in default state on positive self-assessment and resulting self-assessment is expired`() {
        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val onsetDate = LocalDate.now(fixedClock)
            .minusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong() + 1)

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(
                ExplicitDate(onsetDate)
            )
        )

        val actualState = testSubject.readState()
        assertEquals(IsolationState(isolationConfiguration = isolationConfiguration), actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `from default to index state on positive self-assessment and resulting self-assessment not expired`() {
        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val onsetDate = LocalDate.now(fixedClock).minusDays(1)

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(ExplicitDate(onsetDate))
        )

        val actualState = testSubject.readState()
        val expectedState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock), onsetDate)
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        verify {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actualState
        )
    }

    @Test
    fun `from index case caused by positive test result to index case triggered by self-assessment with onset date equal to test end date`() {
        val testEndDate = LocalDate.now(fixedClock)
        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            testResult = positiveTest(testEndDate)
        )
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(OnPositiveSelfAssessment(CannotRememberDate))

        val actualState = testSubject.readState()
        assertEquals(currentState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        newStateIsolationChecks(
            currentState = currentState,
            newState = actualState
        )
    }

    @Test
    fun `from index case caused by positive test result to index case triggered by self-assessment with explicit onset date after test end date`() {
        val testEndDate = LocalDate.now(fixedClock).minusDays(2)
        val testResult = positiveTest(testEndDate)
        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            testResult = testResult
        )
        every { stateProvider.state } returns currentState

        val onsetDate = LocalDate.now(fixedClock)

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(OnPositiveSelfAssessment(ExplicitDate(onsetDate)))

        val actualState = testSubject.readState()
        val expectedState = IsolationState(
            isolationConfiguration = currentState.isolationConfiguration,
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock),
                onsetDate = onsetDate
            ),
            testResult = testResult
        )

        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        newStateIsolationChecks(
            currentState = currentState,
            newState = actualState
        )
    }

    @Test
    fun `from index case caused by positive test result to index case triggered by self-assessment with assumed onset date after test end date`() {
        val testEndDate = LocalDate.now(fixedClock).minusDays(3)
        val testResult = positiveTest(testEndDate)
        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            testResult = testResult
        )
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(OnPositiveSelfAssessment(CannotRememberDate))

        val actualState = testSubject.readState()
        val expectedState = IsolationState(
            isolationConfiguration = currentState.isolationConfiguration,
            selfAssessment = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock),
                onsetDate = null
            ),
            testResult = testResult
        )

        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        newStateIsolationChecks(
            currentState = currentState,
            newState = actualState
        )
    }

    @Test
    fun `stay in index case caused by positive test result when onset date is not stated`() {
        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            testResult = positiveTest()
        )
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(OnPositiveSelfAssessment(NotStated))

        val actualState = testSubject.readState()
        assertEquals(currentState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
    }

    @Test
    fun `stay in current state on positive self-assessment if already isolating as index case with self-assessment`() {
        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
        )
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(CannotRememberDate)
        )

        val actualState = testSubject.readState()
        assertEquals(currentState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        newStateIsolationChecks(
            currentState = currentState,
            newState = actualState
        )
    }

    @Test
    fun `stay in default on positive test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val isolationState = IsolationState(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns isolationState

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val actual = testSubject.readState()
        assertEquals(isolationState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { trackTestResultAnalyticsOnReceive.invoke(testResult, INSIDE_APP) }
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `stay in default on void test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = VOID,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        every { stateProvider.state } returns IsolationState(isolationConfiguration = isolationConfiguration)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val actual = testSubject.readState()
        assertEquals(IsolationState(isolationConfiguration = isolationConfiguration), actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(ReceivedUnconfirmedPositiveTestResult)
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `from default to contact case on exposed notification contact 5 days ago`() {
        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-15T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()
        val expected = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = exposureDate.toLocalDate(ZoneOffset.UTC),
                notificationDate = LocalDate.now(today),
            )
        )
        assertEquals(expected, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(SendExposedNotification, sideEffect)

        verify(exactly = 1) { exposureNotificationHandler.show() }
        verify(exactly = 1) { analyticsEventProcessor.track(StartedIsolation) }
        verify(exactly = 0) { scheduleIsolationHubReminder() }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actual
        )
    }

    @Test
    fun `stay default on exposed notification contact 14 days ago`() {
        every { stateProvider.state } returns IsolationState(isolationConfiguration = isolationConfiguration)

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-06T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()
        val expected = IsolationState(isolationConfiguration = isolationConfiguration)
        assertEquals(expected, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `stay default on exposed notification contact 15 days ago`() {
        every { stateProvider.state } returns IsolationState(isolationConfiguration = isolationConfiguration)

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-05T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()
        val expected = IsolationState(isolationConfiguration = isolationConfiguration)
        assertEquals(expected, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `in index case on exposed notification add contact case`() {
        val currenState = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
            .asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currenState

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actualState = testSubject.readState()
        val expectedState = currenState.copy(
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock)
            )
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(SendExposedNotification, sideEffect)
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currenState,
            newState = actualState
        )
    }

    @Test
    fun `in contact case on exposed notification add contact case`() {
        val currentState = contact().asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(currentState, actual)
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(null, sideEffect)
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actual
        )
    }

    @Test
    fun `stay in index case on exposed notification with confirmed positive test result`() {
        val testEndDate = LocalDate.now(fixedClock).minusDays(3)
        val initialState = positiveTest(testEndDate).asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns initialState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()
        assertEquals(initialState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = initialState,
            newState = actual
        )
    }

    @Test
    fun `add contact case to index case on exposed notification without confirmed positive test result`() {
        val initialState = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
            .asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns initialState

        val testSubject = createIsolationStateMachine(fixedClock)

        val exposureDate = Instant.now(fixedClock)
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()
        val expected = initialState.copy(
            contact = Contact(
                exposureDate = exposureDate.toLocalDate(ZoneOffset.UTC),
                notificationDate = LocalDate.now(fixedClock)
            )
        )
        assertEquals(expected, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(SendExposedNotification, sideEffect)

        verify(exactly = 1) { exposureNotificationHandler.show() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = initialState,
            newState = actual
        )
    }

    @Test
    fun `stay in index case on positive confirmed test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = positiveTest().asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val actual = testSubject.readState()
        assertEquals(expected = currentState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { trackTestResultAnalyticsOnReceive.invoke(testResult, INSIDE_APP) }
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actual
        )
    }

    @Test
    fun `stay in index case on positive unconfirmed test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = true
        )

        val currentState = positiveTest().asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val actual = testSubject.readState()
        assertEquals(expected = currentState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { trackTestResultAnalyticsOnReceive.invoke(testResult, INSIDE_APP) }
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actual
        )
    }

    @Test
    fun `stay in index case on void test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = VOID,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = positiveTest().asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val actual = testSubject.readState()
        assertEquals(expected = currentState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { trackTestResultAnalyticsOnReceive.invoke(testResult, INSIDE_APP) }
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actual
        )
    }

    @Test
    fun `stay in index case on negative test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = positiveTest().asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val newState = testSubject.readState()
        assertEquals(expected = currentState, newState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { trackTestResultAnalyticsOnReceive.invoke(testResult, INSIDE_APP) }
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = newState
        )
    }

    @Test
    fun `add index case to contact case on positive self-assessment and resulting self-assessment not expired`() {
        val contact = contact()
        val isolation = contact().asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val onsetDate = LocalDate.parse("2020-05-19")
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(ExplicitDate(onsetDate))
        )

        val actual = testSubject.readState()
        val expected =
            IsolationState(
                isolationConfiguration = isolationConfiguration,
                selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock), onsetDate),
                contact = contact
            )
        assertEquals(expected, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = isolation,
            newState = actual
        )
    }

    @Test
    fun `add index case to contact case on positive self-assessment and user cannot remember onset date`() {
        val contact = contact()
        val isolation = contact.asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(CannotRememberDate)
        )

        val actual = testSubject.readState()
        val expected =
            IsolationState(
                isolationConfiguration = isolationConfiguration,
                contact = contact,
                selfAssessment = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
            )
        assertEquals(expected, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)

        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = isolation,
            newState = actual
        )
    }

    @Test
    fun `keep contact case on positive test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
            )
        )
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val newState = testSubject.readState()
        assertEquals(expected = currentState, newState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { trackTestResultAnalyticsOnReceive.invoke(testResult, INSIDE_APP) }
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = newState
        )
    }

    @Test
    fun `keep contact case on negative test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
            )
        )
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val newState = testSubject.readState()
        assertEquals(expected = currentState, newState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { trackTestResultAnalyticsOnReceive.invoke(testResult, INSIDE_APP) }
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = newState
        )
    }

    @Test
    fun `keep contact case on void test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = VOID,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock)
            )
        )
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResult(testResult, testOrderType = INSIDE_APP)
        )

        val newState = testSubject.readState()
        assertEquals(expected = currentState, newState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(HandleTestResult(testResult, testOrderType = INSIDE_APP), sideEffect)

        verify { trackTestResultAnalyticsOnReceive.invoke(testResult, INSIDE_APP) }
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { storageBasedUserInbox.notifyChanges() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = newState
        )
    }

    @Test
    fun `stay in default on test result acknowledgement when test isolation result handler returns DoNotTransition with keySharingInfo`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = "123",
            acknowledgedDate = Instant.now(fixedClock)
        )

        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        val currentLogicalState = createIsolationLogicalState(currentState)
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns DoNotTransition(preventKeySubmission = false, keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        assertEquals(expected = currentState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(
            SideEffect.HandleAcknowledgedTestResult(currentLogicalState, testResult, keySharingInfo),
            sideEffect
        )

        verify { trackTestResultAnalyticsOnAcknowledge.invoke(currentLogicalState, testResult) }
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `stay in default on test result acknowledgement when test isolation result handler returns DoNotTransition without keySharingInfo`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        val currentLogicalState = createIsolationLogicalState(currentState)
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        assertEquals(expected = currentState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(
            SideEffect.HandleAcknowledgedTestResult(currentLogicalState, testResult, keySharingInfo = null),
            sideEffect
        )

        verify { trackTestResultAnalyticsOnAcknowledge.invoke(currentLogicalState, testResult) }
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 0) { keySharingInfoProvider setProperty "keySharingInfo" value any<KeySharingInfo>() }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `stay in isolation on test result acknowledgement when test isolation result handler returns DoNotTransition`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )

        val currentState = contact().asIsolation(isolationConfiguration = isolationConfiguration)
        val currentLogicalState = createIsolationLogicalState(currentState)
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns DoNotTransition(preventKeySubmission = false, keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        assertEquals(expected = currentState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(
            SideEffect.HandleAcknowledgedTestResult(currentLogicalState, testResult, keySharingInfo),
            sideEffect
        )

        verify { trackTestResultAnalyticsOnAcknowledge.invoke(currentLogicalState, testResult) }
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState = currentState,
            newState = currentState
        )
    }

    @Test
    fun `transition from default on test result acknowledgement when test isolation result handler returns Transition to active isolation`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )

        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        val currentLogicalState = createIsolationLogicalState(currentState)
        val newState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns TransitionDueToTestResult.Transition(newState.toIsolationInfo(), keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        assertEquals(expected = newState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(
            SideEffect.HandleAcknowledgedTestResult(currentLogicalState, testResult, keySharingInfo),
            sideEffect
        )

        verify { trackTestResultAnalyticsOnAcknowledge.invoke(currentLogicalState, testResult) }
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify { analyticsEventProcessor.track(StartedIsolation) }
        verify(exactly = 0) { scheduleIsolationHubReminder() }

        newStateIsolationChecks(
            currentState = currentState,
            newState = newState
        )
    }

    @Test
    fun `transition from default on test result acknowledgement when test isolation result handler returns Transition to expired isolation`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )

        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        val currentLogicalState = createIsolationLogicalState(currentState)
        val newState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock).minusDays(12),
                notificationDate = LocalDate.now(fixedClock).minusDays(12)
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns TransitionDueToTestResult.Transition(newState.toIsolationInfo(), keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = newState.copy(hasAcknowledgedEndOfIsolation = true)
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(
            SideEffect.HandleAcknowledgedTestResult(currentLogicalState, testResult, keySharingInfo),
            sideEffect
        )
        verify { trackTestResultAnalyticsOnAcknowledge.invoke(currentLogicalState, testResult) }
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `transition from isolation on test result acknowledgement when test isolation result handler returns Transition to active isolation`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )

        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock)
            )
        )
        val currentLogicalState = createIsolationLogicalState(currentState)
        val newState = currentState.copy(
            testResult = AcknowledgedTestResult(
                testEndDate = LocalDate.now(fixedClock),
                acknowledgedDate = LocalDate.now(fixedClock),
                testResult = RelevantVirologyTestResult.POSITIVE,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false,
                confirmedDate = null
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns TransitionDueToTestResult.Transition(newState.toIsolationInfo(), keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)
        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        assertEquals(expected = newState, actual)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(
            SideEffect.HandleAcknowledgedTestResult(currentLogicalState, testResult, keySharingInfo),
            sideEffect
        )

        verify { trackTestResultAnalyticsOnAcknowledge.invoke(currentLogicalState, testResult) }
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateIsolationChecks(
            currentState,
            newState
        )
    }

    @Test
    fun `transition from isolation on test result acknowledgement when test isolation result handler returns Transition to expired isolation`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )
        val keySharingInfo = KeySharingInfo(
            diagnosisKeySubmissionToken = testResult.diagnosisKeySubmissionToken!!,
            acknowledgedDate = Instant.now(fixedClock)
        )

        val currentState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = LocalDate.now(fixedClock).minusDays(12),
                notificationDate = LocalDate.now(fixedClock).minusDays(12)
            )
        )
        val currentLogicalState = createIsolationLogicalState(currentState)
        val newState = currentState.copy(
            testResult = AcknowledgedTestResult(
                testEndDate = LocalDate.now(fixedClock).minusDays(12),
                acknowledgedDate = LocalDate.now(fixedClock),
                testResult = RelevantVirologyTestResult.POSITIVE,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false,
                confirmedDate = null
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState,
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns TransitionDueToTestResult.Transition(newState.toIsolationInfo(), keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = newState.copy(hasAcknowledgedEndOfIsolation = true)
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(
            SideEffect.HandleAcknowledgedTestResult(currentLogicalState, testResult, keySharingInfo),
            sideEffect
        )
        verify { trackTestResultAnalyticsOnAcknowledge.invoke(currentLogicalState, testResult) }
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `reset should put state machine in default state with no history`() {
        val startDate = LocalDate.now(fixedClock)
        val indexCase = IsolationState(
            isolationConfiguration = isolationConfiguration,
            selfAssessment = SelfAssessment(startDate)
        )
        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)
        testSubject.reset()

        assertEquals(IsolationState(isolationConfiguration = isolationConfiguration), testSubject.readState())
        coVerify(exactly = 0) {
            analyticsEventProcessor.track(StartedIsolation)
            scheduleIsolationHubReminder()
        }

        newStateDefaultChecks()
    }

    @Test
    fun `remaining days in isolation is 0 when state is NeverIsolating`() {
        val testSubject = createIsolationStateMachine(fixedClock)
        val logicalState = NeverIsolating(isolationConfiguration = isolationConfiguration, negativeTest = null)

        val remainingDaysInIsolation = testSubject.remainingDaysInIsolation(logicalState)

        assertEquals(0, remainingDaysInIsolation)
    }

    @Test
    fun `remaining days in isolation is number of days until isolation end when in isolation`() {
        val testSubject = createIsolationStateMachine(fixedClock)
        val contactExposureDate = LocalDate.now(fixedClock)
        val contactExpiryDate = contactExposureDate.plusDays(14)
        val logicalState = PossiblyIsolating(
            isolationConfiguration = isolationConfiguration,
            contactCase = ContactCase(
                exposureDate = contactExposureDate,
                notificationDate = contactExposureDate,
                expiryDate = contactExpiryDate
            ),
            startDate = contactExposureDate,
            expiryDate = contactExpiryDate
        )

        val remainingDaysInIsolation = testSubject.remainingDaysInIsolation(logicalState)

        assertEquals(14, remainingDaysInIsolation)
    }

    @Test
    fun `when in contact case isolation and opting out of contact isolation then expire contact isolation and store encounter date in contact case`() {
        val encounterDate = LocalDate.now(fixedClock).minus(3, ChronoUnit.DAYS)
        val notificationDate = LocalDate.now(fixedClock).minus(2, ChronoUnit.DAYS)
        val contactCaseOnlyIsolation = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = Contact(
                exposureDate = encounterDate,
                notificationDate = notificationDate,
                optOutOfContactIsolation = null
            )
        )
        every { stateProvider.state } returns contactCaseOnlyIsolation

        val testSubject = createIsolationStateMachine(fixedClock)
        testSubject.optOutOfContactIsolation(encounterDate, reason = QUESTIONNAIRE)

        val actual = testSubject.readState()
        val expected = contactCaseOnlyIsolation.copy(
            contact = contactCaseOnlyIsolation.contact!!.copy(
                optOutOfContactIsolation = OptOutOfContactIsolation(encounterDate, reason = QUESTIONNAIRE)
            ),
            hasAcknowledgedEndOfIsolation = true
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `when not in isolation, acknowledgeIsolationExpiration does not transition`() {
        val currentState = IsolationState(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        testSubject.acknowledgeIsolationExpiration()

        val newState = testSubject.readState()
        assertEquals(expected = currentState, newState)
    }

    @Test
    fun `when isolation expired, acknowledgeIsolationExpiration marks expiration acknowledged`() {
        val contact = contact(
            startDate = LocalDate.now(fixedClock).minusDays(14)
        )
        val currentState = contact.asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        testSubject.acknowledgeIsolationExpiration()

        val expectedState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = contact,
            hasAcknowledgedEndOfIsolation = true
        )
        val newState = testSubject.readState()
        assertEquals(expected = expectedState, newState)
    }

    @Test
    fun `when isolation active, acknowledgeIsolationExpiration marks expiration acknowledged`() {
        // The user is notified about isolation expiration the day before expiration at 9pm, so they
        // can actually acknowledge the expiration before the isolation really expires.

        val contact = contact()
        val currentState = contact.asIsolation(isolationConfiguration = isolationConfiguration)
        every { stateProvider.state } returns currentState

        val testSubject = createIsolationStateMachine(fixedClock)
        testSubject.acknowledgeIsolationExpiration()

        val expectedState = IsolationState(
            isolationConfiguration = isolationConfiguration,
            contact = contact,
            hasAcknowledgedEndOfIsolation = true
        )
        val newState = testSubject.readState()
        assertEquals(expected = expectedState, newState)
    }

    private fun newStateDefaultChecks() {
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify { exposureNotificationHandler.cancel() }
        verify { isolationHubReminderAlarmController.cancel() }
    }

    private fun newStateIsolationChecks(currentState: IsolationState, newState: IsolationState) {
        val newLogicalState = createIsolationLogicalState(newState)
        assertTrue(newLogicalState is PossiblyIsolating)

        val currentLogicalState = createIsolationLogicalState(currentState)
        verify { alarmController.setupExpirationCheck(currentLogicalState, newLogicalState) }
    }

    private fun positiveTest(
        testEndDate: LocalDate = LocalDate.now(fixedClock)
    ): AcknowledgedTestResult =
        AcknowledgedTestResult(
            testEndDate = testEndDate,
            acknowledgedDate = LocalDate.now(fixedClock),
            testResult = RelevantVirologyTestResult.POSITIVE,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )

    private fun contact(
        startDate: LocalDate = LocalDate.now(fixedClock),
    ): Contact =
        Contact(
            exposureDate = startDate,
            notificationDate = startDate,
        )

    private fun createIsolationStateMachine(clock: Clock): IsolationStateMachine {
        return IsolationStateMachine(
            stateProvider,
            notificationProvider,
            unacknowledgedTestResultsProvider,
            testResultIsolationHandler,
            storageBasedUserInbox,
            alarmController,
            clock,
            analyticsEventProcessor,
            exposureNotificationHandler,
            keySharingInfoProvider,
            createIsolationLogicalState,
            trackTestResultAnalyticsOnReceive,
            trackTestResultAnalyticsOnAcknowledge,
            scheduleIsolationHubReminder,
            isolationHubReminderAlarmController,
            createIsolationState
        )
    }
}
