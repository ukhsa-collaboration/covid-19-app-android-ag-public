package uk.nhs.nhsx.covid19.android.app.state

import com.tinder.StateMachine.Transition
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DeclaredNegativeResultFromDct
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfoProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class IsolationStateMachineTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val stateProvider = mockk<StateStorage>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val alarmController = mockk<IsolationExpirationAlarmController>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxed = true)
    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxed = true)
    private val testResultIsolationHandler = mockk<TestResultIsolationHandler>(relaxed = true)
    private val analyticsEventTracker = mockk<AnalyticsEventTracker>(relaxUnitFun = true)
    private val exposureNotificationHandler = mockk<ExposureNotificationHandler>(relaxUnitFun = true)
    private val keySharingInfoProvider = mockk<KeySharingInfoProvider>(relaxUnitFun = true)
    private val createSelfAssessmentIndexCase = mockk<CreateSelfAssessmentIndexCase>()

    private val durationDays = DurationDays()

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `from default to index case on positive self-assessment and user cannot remember onset date`() {
        val currentState = IsolationState(isolationConfiguration = durationDays)
        every { stateProvider.state } returns currentState

        val selfAssessmentTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
        val expiryDateForSymptomatic = LocalDate.now(fixedClock).plusDays(2)
        val indexCase = IndexCase(
            isolationTrigger = selfAssessmentTrigger,
            expiryDate = expiryDateForSymptomatic
        )
        every { createSelfAssessmentIndexCase(currentState.asLogical(), selfAssessmentTrigger) } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(
                CannotRememberDate
            )
        )

        val actualState = testSubject.readState()

        val expectedState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = indexCase
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
        verify(exactly = 1) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actualState
        )
    }

    @Test
    fun `stay in default state on positive self-assessment and resulting self-assessment is expired`() {
        val currentState = IsolationState(isolationConfiguration = durationDays)
        every { stateProvider.state } returns currentState

        val onsetDate = LocalDate.now(fixedClock).minusDays(5)
        val selfAssessmentTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock), onsetDate)
        val expiryDateForSymptomatic = LocalDate.now(fixedClock).minusDays(1)
        val indexCase = IndexCase(
            isolationTrigger = selfAssessmentTrigger,
            expiryDate = expiryDateForSymptomatic
        )
        every { createSelfAssessmentIndexCase(currentState.asLogical(), selfAssessmentTrigger) } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(
                SelectedDate.ExplicitDate(onsetDate)
            )
        )

        val actualState = testSubject.readState()

        assertEquals(IsolationState(isolationConfiguration = durationDays), actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `from default to index state on positive self-assessment and resulting self-assessment not expired`() {
        val currentState = IsolationState(isolationConfiguration = durationDays)
        every { stateProvider.state } returns currentState

        val onsetDate = LocalDate.now(fixedClock).minusDays(1)
        val selfAssessmentTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock), onsetDate)
        val expiryDateForSymptomatic = LocalDate.now(fixedClock).plusDays(2)
        val indexCase = IndexCase(
            isolationTrigger = selfAssessmentTrigger,
            expiryDate = expiryDateForSymptomatic
        )
        every { createSelfAssessmentIndexCase(currentState.asLogical(), selfAssessmentTrigger) } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(SelectedDate.ExplicitDate(onsetDate))
        )

        val actualState = testSubject.readState()

        val expectedState = IsolationState(
            indexInfo = indexCase,
            isolationConfiguration = durationDays
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
        verify(exactly = 1) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actualState
        )
    }

    @Test
    fun `stay in current state on positive self-assessment if already isolating as index case with positive test`() {
        val currentState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = positiveTest(5)
        )
        every { stateProvider.state } returns currentState

        val selfAssessmentTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
        val expiryDateForSymptomatic = LocalDate.now(fixedClock).plusDays(2)
        val indexCase = IndexCase(
            isolationTrigger = selfAssessmentTrigger,
            expiryDate = expiryDateForSymptomatic
        )
        every { createSelfAssessmentIndexCase(currentState.asLogical(), selfAssessmentTrigger) } returns indexCase

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
    fun `stay in current state on positive self-assessment if already isolating as index case with self-assessment`() {
        val currentState = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = selfAssessment(expiryDate = LocalDate.now(fixedClock).plusDays(5))
        )
        every { stateProvider.state } returns currentState

        val selfAssessmentTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
        val expiryDateForSymptomatic = LocalDate.now(fixedClock).plusDays(2)
        val indexCase = IndexCase(
            isolationTrigger = selfAssessmentTrigger,
            expiryDate = expiryDateForSymptomatic
        )
        every { createSelfAssessmentIndexCase(currentState.asLogical(), selfAssessmentTrigger) } returns indexCase

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

        every { stateProvider.state } returns IsolationState(isolationConfiguration = durationDays)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(IsolationState(isolationConfiguration = durationDays), actual)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

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

        every { stateProvider.state } returns IsolationState(isolationConfiguration = durationDays)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(IsolationState(isolationConfiguration = durationDays), actual)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `from default to contact case on exposed notification contact 5 days ago`() {
        val currentState = IsolationState(isolationConfiguration = durationDays)
        every { stateProvider.state } returns currentState

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-15T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val exposureDay = exposureDate.atZone(ZoneOffset.UTC).toLocalDate()
        val expiryDate = exposureDay
            .plusDays(durationDays.contactCase.toLong())
        val expected = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = exposureDay,
                notificationDate = LocalDate.now(today),
                expiryDate = expiryDate
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SendExposedNotification, sideEffect)
        verify(exactly = 1) { exposureNotificationHandler.show() }
        verify(exactly = 1) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actual
        )
    }

    @Test
    fun `stay default on exposed notification contact 14 days ago`() {
        every { stateProvider.state } returns IsolationState(isolationConfiguration = durationDays)

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-06T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val expected = IsolationState(isolationConfiguration = durationDays)
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `stay default on exposed notification contact 15 days ago`() {
        every { stateProvider.state } returns IsolationState(isolationConfiguration = durationDays)

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-05T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val expected = IsolationState(isolationConfiguration = durationDays)
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `in index case on exposed notification add contact case`() {
        val case = isolationSelfAssessment(
            expiryDate = LocalDate.now(fixedClock).plusDays(3)
        )
        every { stateProvider.state } returns case

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(
            case.copy(
                contactCase = ContactCase(
                    exposureDate = LocalDate.now(fixedClock),
                    notificationDate = LocalDate.now(fixedClock),
                    expiryDate = LocalDate.now(fixedClock)
                        .plusDays(durationDays.contactCase.toLong())
                )
            ),
            actual
        )
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(SendExposedNotification, sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = case,
            newState = actual
        )
    }

    @Test
    fun `in contact case on exposed notification add contact case`() {
        val case = isolationContactCase(expiryDaysFromStartDate = 3)
        every { stateProvider.state } returns case

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(case, actual)
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(null, sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = case,
            newState = actual
        )
    }

    @Test
    fun `stay in index case on exposed notification with confirmed positive test result`() {
        val initialState = isolationPositiveTest(3)
        every { stateProvider.state } returns initialState

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(initialState, actual)
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = initialState,
            newState = actual
        )
    }

    @Test
    fun `add contact case to index case on exposed notification without confirmed positive test result`() {
        val initialState = isolationSelfAssessment(
            expiryDate = LocalDate.now(fixedClock).plusDays(3)
        )
        every { stateProvider.state } returns initialState

        val testSubject = createIsolationStateMachine(fixedClock)

        val exposureDate = Instant.now(fixedClock)
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val exposureDay = exposureDate.atZone(ZoneOffset.UTC).toLocalDate()
        val expiryDate = exposureDay
            .plusDays(durationDays.contactCase.toLong())
        val expected = initialState.copy(
            contactCase = ContactCase(
                exposureDate = exposureDay,
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = expiryDate
            )
        )

        assertEquals(expected, actual)
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(SendExposedNotification, sideEffect)
        verify(exactly = 1) { exposureNotificationHandler.show() }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

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

        val state = isolationPositiveTest(3)
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        val expected = state.copy()
        assertEquals(expected, actual)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = state,
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

        val state = isolationPositiveTest(3)
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        val expected = state.copy()
        assertEquals(expected, actual)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = state,
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

        val state = isolationPositiveTest(3)
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        val expected = state.copy()
        assertEquals(expected, actual)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = state,
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

        val isolation = isolationPositiveTest(expiryDaysFromStartDate = 5)
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val sideEffect = (transition as Transition.Valid).sideEffect

        val expected = isolation.copy()
        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = isolation,
            newState = newState
        )
    }

    @Test
    fun `add index case to contact case on positive self-assessment and resulting self-assessment not expired`() {
        val contactCase = contactCase(365)
        val isolation = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = contactCase
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val onsetDate = LocalDate.parse("2020-05-19")
        val selfAssessmentTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock), onsetDate)
        val expiryDateForSymptomatic = LocalDate.now(fixedClock).plusDays(2)
        val indexCase = IndexCase(
            isolationTrigger = selfAssessmentTrigger,
            expiryDate = expiryDateForSymptomatic
        )
        every { createSelfAssessmentIndexCase(isolation.asLogical(), selfAssessmentTrigger) } returns indexCase

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(SelectedDate.ExplicitDate(onsetDate))
        )

        val actual = testSubject.readState()

        val expected =
            IsolationState(
                isolationConfiguration = durationDays,
                indexInfo = indexCase,
                contactCase = contactCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = isolation,
            newState = actual
        )
    }

    @Test
    fun `add index case to contact case on positive self-assessment and user cannot remember onset date`() {
        val contactCase = contactCase(expiryDaysFromStartDate = 3)
        val isolation = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = contactCase
        )
        every { stateProvider.state } returns isolation

        val selfAssessmentTrigger = SelfAssessment(selfAssessmentDate = LocalDate.now(fixedClock))
        val expiryDateForSymptomatic = LocalDate.now(fixedClock).plusDays(2)
        val indexCase = IndexCase(
            isolationTrigger = selfAssessmentTrigger,
            expiryDate = expiryDateForSymptomatic
        )
        every { createSelfAssessmentIndexCase(isolation.asLogical(), selfAssessmentTrigger) } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(CannotRememberDate)
        )

        val actual = testSubject.readState()

        val expected =
            IsolationState(
                isolationConfiguration = durationDays,
                contactCase = contactCase,
                indexInfo = indexCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

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

        val startDate = LocalDate.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        val isolation = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = startDate,
                notificationDate = startDate,
                expiryDate = expiryDate
            )
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = LocalDate.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = startDateExpected,
                notificationDate = startDateExpected,
                expiryDate = expiryDateExpected
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = isolation,
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

        val startDate = LocalDate.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        val isolation = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = startDate,
                notificationDate = startDate,
                expiryDate = expiryDate
            )
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = LocalDate.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = startDateExpected,
                notificationDate = startDateExpected,
                expiryDate = expiryDateExpected
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = isolation,
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

        val startDate = LocalDate.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        val isolation = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = startDate,
                notificationDate = startDate,
                expiryDate = expiryDate
            )
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = LocalDate.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = startDateExpected,
                notificationDate = startDateExpected,
                expiryDate = expiryDateExpected
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { unacknowledgedTestResultsProvider.add(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = isolation,
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
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )

        val currentState = IsolationState(isolationConfiguration = durationDays)
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState.asLogical(),
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns DoNotTransition(preventKeySubmission = false, keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = currentState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SideEffect.HandleAcknowledgedTestResult(testResult, keySharingInfo), sideEffect)
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

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

        val currentState = IsolationState(isolationConfiguration = durationDays)
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState.asLogical(),
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = currentState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SideEffect.HandleAcknowledgedTestResult(testResult, keySharingInfo = null), sideEffect)
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 0) { keySharingInfoProvider setProperty "keySharingInfo" value any<KeySharingInfo>() }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

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
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )

        val currentState = isolationContactCase(expiryDaysFromStartDate = 3)
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState.asLogical(),
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns DoNotTransition(preventKeySubmission = false, keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = currentState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SideEffect.HandleAcknowledgedTestResult(testResult, keySharingInfo), sideEffect)
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = currentState
        )
    }

    @Test
    fun `transition from default on test result acknowledgement when test isolation result handler returns Transition`() {
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
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )

        val currentState = IsolationState(isolationConfiguration = durationDays)
        val newState = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(10)
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState.asLogical(),
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns TransitionDueToTestResult.Transition(newState, keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = newState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SideEffect.HandleAcknowledgedTestResult(testResult, keySharingInfo), sideEffect)
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = newState
        )
    }

    @Test
    fun `transition from isolation on test result acknowledgement when test isolation result handler returns Transition`() {
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
            acknowledgedDate = Instant.now(fixedClock),
            testKitType = testResult.testKitType,
            requiresConfirmatoryTest = testResult.requiresConfirmatoryTest
        )

        val currentState = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(10)
            )
        )
        val newState = currentState.copy(
            indexInfo = IndexCase(
                isolationTrigger = PositiveTestResult(testEndDate = LocalDate.now(fixedClock)),
                testResult = AcknowledgedTestResult(
                    testEndDate = LocalDate.now(fixedClock),
                    acknowledgedDate = LocalDate.now(fixedClock),
                    testResult = RelevantVirologyTestResult.POSITIVE,
                    testKitType = LAB_RESULT,
                    requiresConfirmatoryTest = false,
                    confirmedDate = null
                ),
                expiryDate = LocalDate.now(fixedClock)
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                currentState.asLogical(),
                testResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns TransitionDueToTestResult.Transition(newState, keySharingInfo)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = newState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SideEffect.HandleAcknowledgedTestResult(testResult, keySharingInfo), sideEffect)
        verify(exactly = 1) { unacknowledgedTestResultsProvider.remove(testResult) }
        verify(exactly = 1) { keySharingInfoProvider setProperty "keySharingInfo" value eq(keySharingInfo) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState,
            newState
        )
    }

    @Test
    fun `reset should put state machine in default state with no history`() {
        val startDate = LocalDate.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val indexCase = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(startDate),
                expiryDate = expiryDate,
            )
        )

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        testSubject.reset()

        assertEquals(IsolationState(isolationConfiguration = durationDays), testSubject.readState())
        coVerify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `isolation state expiry date`() {
        val expiryDateYesterday = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        ).asLogical()

        val expiryDateToday = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock)
            )
        ).asLogical()

        val expiryDateTomorrow = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        ).asLogical()

        assertTrue(expiryDateYesterday is PossiblyIsolating)
        assertTrue(expiryDateYesterday.hasExpired(fixedClock))

        assertTrue(expiryDateToday is PossiblyIsolating)
        assertTrue(expiryDateToday.hasExpired(fixedClock))

        assertTrue(expiryDateTomorrow is PossiblyIsolating)
        assertFalse(expiryDateTomorrow.hasExpired(fixedClock))
    }

    @Test
    fun `remaining days in isolation is 0 when state is Default`() {
        val testSubject = createIsolationStateMachine(fixedClock)
        val state = IsolationState(isolationConfiguration = durationDays)
        val logicalState = state.asLogical()

        val remainingDaysInIsolation = testSubject.remainingDaysInIsolation(logicalState)

        assertEquals(0, remainingDaysInIsolation)
    }

    @Test
    fun `remaining days in isolation is number of days until isolation end when in isolation`() {
        val testSubject = createIsolationStateMachine(fixedClock)
        val state = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = LocalDate.now(fixedClock),
                notificationDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(14)
            )
        )
        val logicalState = state.asLogical()

        val remainingDaysInIsolation = testSubject.remainingDaysInIsolation(logicalState)

        assertEquals(14, remainingDaysInIsolation)
    }

    @Test
    fun `when in contact case only and opt-in to daily contact testing, transition to default and store contact case isolation and opt-in date`() {
        val startDate = LocalDate.now(fixedClock).minus(3, ChronoUnit.DAYS)
        val notificationDate = LocalDate.now(fixedClock).minus(2, ChronoUnit.DAYS)
        val expiryDate = LocalDate.now(fixedClock).plusDays(12)
        val contactCaseOnlyIsolation = IsolationState(
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                exposureDate = startDate,
                notificationDate = notificationDate,
                expiryDate = expiryDate,
                dailyContactTestingOptInDate = null
            )
        )

        every { stateProvider.state } returns contactCaseOnlyIsolation

        val testSubject = createIsolationStateMachine(fixedClock)

        testSubject.optInToDailyContactTesting()

        val actual = testSubject.readState()
        val expected = contactCaseOnlyIsolation.copy(
            contactCase = contactCaseOnlyIsolation.contactCase!!.copy(
                expiryDate = LocalDate.now(fixedClock),
                dailyContactTestingOptInDate = LocalDate.now(fixedClock)
            ),
            hasAcknowledgedEndOfIsolation = true
        )

        verify(exactly = 1) { analyticsEventTracker.track(DeclaredNegativeResultFromDct) }

        assertEquals(expected, actual)
    }

    @Test
    fun `when other than contact case only and opt-in to daily contact testing do not transition`() {
        val startDate = LocalDate.now(fixedClock).minus(3, ChronoUnit.DAYS)
        val notificationDate = LocalDate.now(fixedClock).minus(2, ChronoUnit.DAYS)
        val expiryDate = LocalDate.now(fixedClock).plusDays(12)
        val combinedIsolation = IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(
                    selfAssessmentDate = LocalDate.parse("2020-05-22"),
                    onsetDate = LocalDate.parse("2020-05-20")
                ),
                expiryDate = expiryDate.minusDays(3),
            ),
            contactCase = ContactCase(
                exposureDate = startDate,
                notificationDate = notificationDate,
                expiryDate = expiryDate,
                dailyContactTestingOptInDate = null
            )
        )

        every { stateProvider.state } returns combinedIsolation

        val testSubject = createIsolationStateMachine(fixedClock)

        testSubject.optInToDailyContactTesting()

        verify(exactly = 0) { analyticsEventTracker.track(DeclaredNegativeResultFromDct) }

        val actual = testSubject.readState()

        assertEquals(expected = combinedIsolation, actual)
    }

    private fun newStateDefaultChecks() {
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify { exposureNotificationHandler.cancel() }
    }

    private fun newStateIsolationChecks(currentState: IsolationState, newState: IsolationState) {
        val newIsolationLogicalState = newState.asLogical()
        assertTrue(newIsolationLogicalState is PossiblyIsolating)
        verify { alarmController.setupExpirationCheck(currentState.asLogical(), newIsolationLogicalState) }
    }

    private fun positiveTest(expiryDaysFromStartDate: Long): IndexCase {
        val expiryDate = LocalDate.now(fixedClock).plusDays(expiryDaysFromStartDate)
        val testResult = AcknowledgedTestResult(
            testEndDate = LocalDate.now(fixedClock),
            acknowledgedDate = LocalDate.now(fixedClock),
            testResult = RelevantVirologyTestResult.POSITIVE,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )
        return IndexCase(
            isolationTrigger = PositiveTestResult(LocalDate.now(fixedClock).minusDays(3)),
            testResult = testResult,
            expiryDate = expiryDate
        )
    }

    private fun selfAssessment(expiryDate: LocalDate, onsetDate: LocalDate? = null): IndexCase =
        IndexCase(
            isolationTrigger = SelfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock),
                onsetDate = onsetDate
            ),
            expiryDate = expiryDate
        )

    private fun contactCase(expiryDaysFromStartDate: Long): ContactCase {
        val startDate = LocalDate.now(fixedClock)
        val expiryDate = startDate.plusDays(expiryDaysFromStartDate)
        return ContactCase(
            exposureDate = startDate,
            notificationDate = startDate,
            expiryDate = expiryDate
        )
    }

    private fun isolationPositiveTest(expiryDaysFromStartDate: Long): IsolationState =
        IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = positiveTest(expiryDaysFromStartDate)
        )

    private fun isolationSelfAssessment(expiryDate: LocalDate): IsolationState =
        IsolationState(
            isolationConfiguration = durationDays,
            indexInfo = selfAssessment(expiryDate)
        )

    private fun isolationContactCase(expiryDaysFromStartDate: Long): IsolationState =
        IsolationState(
            isolationConfiguration = durationDays,
            contactCase = contactCase(expiryDaysFromStartDate)
        )

    private fun createIsolationStateMachine(clock: Clock): IsolationStateMachine {
        return IsolationStateMachine(
            stateProvider,
            notificationProvider,
            isolationConfigurationProvider,
            unacknowledgedTestResultsProvider,
            testResultIsolationHandler,
            userInbox,
            alarmController,
            clock,
            analyticsEventTracker,
            exposureNotificationHandler,
            keySharingInfoProvider,
            createSelfAssessmentIndexCase
        )
    }
}
