package uk.nhs.nhsx.covid19.android.app.state

import com.tinder.StateMachine.Transition
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DeclaredNegativeResultFromDct
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventTracker
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.AcknowledgeTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.ClearAcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransitionButStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Ignore
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.TransitionAndStoreTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultHandler
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultStorageOperation.Overwrite
import java.time.Clock
import java.time.Duration
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
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val testResultHandler = mockk<TestResultHandler>(relaxed = true)
    private val testResultIsolationHandler = mockk<TestResultIsolationHandler>(relaxed = true)
    private val analyticsEventTracker = mockk<AnalyticsEventTracker>(relaxUnitFun = true)
    private val exposureNotificationHandler = mockk<ExposureNotificationHandler>(relaxUnitFun = true)

    private val durationDays =
        DurationDays(
            contactCase = 14,
            indexCaseSinceSelfDiagnosisOnset = 5,
            indexCaseSinceSelfDiagnosisUnknownOnset = 3,
            maxIsolation = 21,
            pendingTasksRetentionPeriod = 14
        )

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `from default to index case on positive self assessment and user cannot remember onset date`() {
        every { stateProvider.state } returns Default()

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(
                CannotRememberDate
            )
        )

        val actualState = testSubject.readState()

        val startDateForSymptomatic = Instant.now(fixedClock)
        val expiryDateForSymptomatic = LocalDate.now(fixedClock).plus(
            durationDays.indexCaseSinceSelfDiagnosisUnknownOnset.toLong(),
            ChronoUnit.DAYS
        )
        val expectedState = Isolation(
            isolationStart = startDateForSymptomatic,
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-05-19"),
                expiryDate = expiryDateForSymptomatic,
                selfAssessment = true
            )
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(ClearAcknowledgedTestResult, sideEffect)
        verify(exactly = 1) { analyticsEventTracker.track(StartedIsolation) }
        verify(exactly = 1) { relevantTestResultProvider.clear() }

        newStateIsolationChecks(
            currentState = Default(),
            newState = actualState
        )
    }

    @Test
    fun `stay in default state on positive self assessment and onset date is too long ago`() {
        every { stateProvider.state } returns Default()

        val onsetDate = LocalDate.now(fixedClock)
            .minusDays(durationDays.indexCaseSinceSelfDiagnosisOnset.toLong() + 1)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(
                SelectedDate.ExplicitDate(onsetDate)
            )
        )

        val actualState = testSubject.readState()

        assertEquals(Default(), actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify(exactly = 0) { relevantTestResultProvider.clear() }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `from default to index state on positive self assessment and onset date is recent`() {
        every { stateProvider.state } returns Default()

        val daysBeforeToday = 1L
        val onsetDate = LocalDate.now(fixedClock).minusDays(daysBeforeToday)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(SelectedDate.ExplicitDate(onsetDate))
        )

        val actualState = testSubject.readState()

        val expiryDateForSymptomatic =
            onsetDate.plusDays(durationDays.indexCaseSinceSelfDiagnosisOnset.toLong() - daysBeforeToday + 1)
        val expectedState = Isolation(
            indexCase = IndexCase(
                symptomsOnsetDate = onsetDate,
                expiryDate = expiryDateForSymptomatic,
                selfAssessment = true
            ),
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = durationDays
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(ClearAcknowledgedTestResult, sideEffect)
        verify(exactly = 1) { analyticsEventTracker.track(StartedIsolation) }
        verify(exactly = 1) { relevantTestResultProvider.clear() }

        newStateIsolationChecks(
            currentState = Default(),
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

        every { stateProvider.state } returns Default()

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(Default(), actual)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultHandler.onTestResultReceived(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
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

        every { stateProvider.state } returns Default()

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(Default(), actual)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultHandler.onTestResultReceived(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `from default to contact case on exposed notification contact 5 days ago`() {
        every { stateProvider.state } returns Default()

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-15T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val isolationStartDate = Instant.now(today)
        val expiryDate = exposureDate.atZone(ZoneOffset.UTC).toLocalDate()
            .plusDays(durationDays.contactCase.toLong())
        val expected = Isolation(
            isolationStart = isolationStartDate,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = exposureDate,
                notificationDate = Instant.now(today),
                expiryDate = expiryDate
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SendExposedNotification, sideEffect)
        verify(exactly = 1) { exposureNotificationHandler.show() }
        verify(exactly = 1) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = Default(),
            newState = actual
        )
    }

    @Test
    fun `stay default on exposed notification contact 14 days ago`() {
        every { stateProvider.state } returns Default()

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-06T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val expected = Default()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `stay default on exposed notification contact 15 days ago`() {
        every { stateProvider.state } returns Default()

        val today = Clock.fixed(Instant.parse("2020-05-20T10:00:00Z"), ZoneOffset.UTC)
        val testSubject = createIsolationStateMachine(today)

        val exposureDate = Instant.parse("2020-05-05T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val expected = Default()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `in index case on exposed notification add contact case`() {
        val case = isolationStateWithIndexCase(3, false)
        every { stateProvider.state } returns isolationStateWithIndexCase(3, false)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(
            case.copy(
                contactCase = ContactCase(
                    startDate = Instant.now(fixedClock),
                    notificationDate = Instant.now(fixedClock),
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
        val case = isolationStateWithContactCase(expiryDaysFromStartDate = 3)
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
        val initialState = isolationStateWithIndexCase(3, false)
        every { stateProvider.state } returns isolationStateWithIndexCase(3, false)

        mockkStatic("uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachineKt")
        every { initialState.hasConfirmedPositiveTestResult(testResultHandler) } returns true

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
        val initialState = isolationStateWithIndexCase(3, false)
        every { stateProvider.state } returns isolationStateWithIndexCase(3, false)

        mockkStatic("uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachineKt")
        every { initialState.hasConfirmedPositiveTestResult(testResultHandler) } returns false

        val testSubject = createIsolationStateMachine(fixedClock)

        val exposureDate = Instant.now(fixedClock)
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val expiryDate = exposureDate.atZone(ZoneOffset.UTC).toLocalDate()
            .plusDays(durationDays.contactCase.toLong())
        val expected = initialState.copy(
            contactCase = ContactCase(
                startDate = exposureDate,
                notificationDate = Instant.now(fixedClock),
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

        val state = isolationStateWithIndexCase(3, false)
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
        verify { testResultHandler.onTestResultReceived(testResult) }
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

        val state = isolationStateWithIndexCase(3, false)
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
        verify { testResultHandler.onTestResultReceived(testResult) }
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

        val state = isolationStateWithIndexCase(3, false)
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
        verify { testResultHandler.onTestResultReceived(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = state,
            newState = actual
        )
    }

    @Test
    fun `stay in index case to default on negative test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(5)
        val isolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-05-20"),
                expiryDate = expiryDate,
                selfAssessment = false
            )
        )
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
        verify { testResultHandler.onTestResultReceived(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = isolation,
            newState = newState
        )
    }

    @Test
    fun `index case with previous contact case stays index case on negative test result`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = NEGATIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val state = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.parse("2020-05-22")
            ),
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-05-20"),
                expiryDate = LocalDate.parse("2020-05-22"),
                selfAssessment = false
            )
        )
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(state, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultHandler.onTestResultReceived(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = state,
            newState = newState
        )
    }

    @Test
    fun `add index case to contact case on positive self assessment and contact case finishes later than index`() {
        val contactCaseStartDate = Instant.now(fixedClock)
        val contactCaseExpiryDate = LocalDate.now(fixedClock).plusDays(365)
        val contactCase = ContactCase(
            startDate = contactCaseStartDate,
            notificationDate = null,
            expiryDate = contactCaseExpiryDate
        )
        val isolation = Isolation(
            isolationStart = contactCaseStartDate,
            isolationConfiguration = durationDays,
            contactCase = contactCase
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val startDate = LocalDate.parse("2020-05-19")
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(SelectedDate.ExplicitDate(startDate))
        )

        val actual = testSubject.readState()

        val startDateForIndex = Instant.now(fixedClock)
        val expected =
            Isolation(
                isolationStart = startDateForIndex,
                isolationConfiguration = durationDays,
                indexCase = IndexCase(
                    symptomsOnsetDate = startDate,
                    expiryDate = startDate.plusDays(23),
                    selfAssessment = true
                ),
                contactCase = contactCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(ClearAcknowledgedTestResult, sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify(exactly = 1) { relevantTestResultProvider.clear() }

        newStateIsolationChecks(
            currentState = isolation,
            newState = actual
        )
    }

    @Test
    fun `add index case to contact case on positive self assessment and user cannot remember onset date`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val contactCase = ContactCase(
            startDate = startDate,
            notificationDate = null,
            expiryDate = expiryDate
        )
        val isolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            contactCase = contactCase
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(CannotRememberDate)
        )

        val actual = testSubject.readState()

        val startDateForIndex = Instant.now(fixedClock)
        val expiryDateForIndex = LocalDate.now(fixedClock)
            .plusDays(durationDays.indexCaseSinceSelfDiagnosisUnknownOnset.toLong())
        val expected =
            Isolation(
                isolationStart = startDateForIndex,
                isolationConfiguration = durationDays,
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.parse("2020-05-19"),
                    expiryDate = expiryDateForIndex,
                    selfAssessment = true
                ),
                contactCase = contactCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(ClearAcknowledgedTestResult, sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify(exactly = 1) { relevantTestResultProvider.clear() }

        newStateIsolationChecks(
            currentState = isolation,
            newState = actual
        )
    }

    // Note: this shouldn't actually happen since it's not possible to perform a self-assessment when already in isolation as an index case
    @Test
    fun `from index case with positive test to index case with self assessment on positive self assessment and user cannot remember onset date`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val indexCase = IndexCase(
            symptomsOnsetDate = LocalDate.parse("2020-05-19"),
            expiryDate = expiryDate,
            selfAssessment = false
        )
        val isolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            indexCase = indexCase
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(CannotRememberDate)
        )

        val actual = testSubject.readState()

        val startDateForIndex = Instant.now(fixedClock)
        val expiryDateForIndex = LocalDate.now(fixedClock)
            .plusDays(durationDays.indexCaseSinceSelfDiagnosisUnknownOnset.toLong())
        val expected =
            Isolation(
                isolationStart = startDateForIndex,
                isolationConfiguration = durationDays,
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.parse("2020-05-19"),
                    expiryDate = expiryDateForIndex,
                    selfAssessment = true
                )
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify(exactly = 0) { relevantTestResultProvider.clear() }

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

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        val isolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDate,
                notificationDate = null,
                expiryDate = expiryDate
            )
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            isolationStart = startDateExpected,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDateExpected,
                notificationDate = null,
                expiryDate = expiryDateExpected
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultHandler.onTestResultReceived(testResult) }
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

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        val isolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDate,
                notificationDate = null,
                expiryDate = expiryDate
            )
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            isolationStart = startDateExpected,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDateExpected,
                notificationDate = null,
                expiryDate = expiryDateExpected
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultHandler.onTestResultReceived(testResult) }
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

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        val isolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDate,
                notificationDate = null,
                expiryDate = expiryDate
            )
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            isolationStart = startDateExpected,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDateExpected,
                notificationDate = null,
                expiryDate = expiryDateExpected
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultHandler.onTestResultReceived(testResult) }
        verify(exactly = 0) { analyticsEventTracker.track(ReceivedUnconfirmedPositiveTestResult) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = isolation,
            newState = newState
        )
    }

    @Test
    fun `stay in default on test result acknowledgement when test isolation result handler returns Ignore`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = Default()
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResult(
                currentState,
                testResult
            )
        } returns Ignore(preventKeySubmission = false)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = currentState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult, TestResultStorageOperation.Ignore), sideEffect)
        verify(exactly = 1) { testResultHandler.acknowledge(testResult, expected.symptomsOnsetDate, TestResultStorageOperation.Ignore) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `stay in isolation on test result acknowledgement when test isolation result handler returns Ignore`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = isolationStateWithContactCase(expiryDaysFromStartDate = 3)
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResult(
                currentState,
                testResult
            )
        } returns Ignore(preventKeySubmission = false)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = currentState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult, TestResultStorageOperation.Ignore), sideEffect)
        verify(exactly = 1) { testResultHandler.acknowledge(testResult, expected.symptomsOnsetDate, TestResultStorageOperation.Ignore) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = actual
        )
    }

    @Test
    fun `transition from default on test result acknowledgement when test isolation result handler returns TransitionAndSaveTestResult`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = Default()
        val newState = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(10)
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResult(
                currentState,
                testResult
            )
        } returns TransitionAndStoreTestResult(newState, testResultStorageOperation = Overwrite)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = newState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult, testResultStorageOperation = Overwrite), sideEffect)
        verify(exactly = 1) { testResultHandler.acknowledge(testResult, expected.symptomsOnsetDate, testResultStorageOperation = Overwrite) }
        verify { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = newState
        )
    }

    @Test
    fun `transition from isolation on test result acknowledgement when test isolation result handler returns TransitionAndSaveTestResult`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(10)
            )
        )
        val newState = currentState.copy(
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(),
                expiryDate = LocalDate.now(),
                selfAssessment = false
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResult(
                currentState,
                testResult
            )
        } returns TransitionAndStoreTestResult(newState, testResultStorageOperation = Overwrite)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = newState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult, testResultStorageOperation = Overwrite), sideEffect)
        verify(exactly = 1) { testResultHandler.acknowledge(testResult, expected.symptomsOnsetDate, testResultStorageOperation = Overwrite) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = newState
        )
    }

    @Test
    fun `do not transition from default on test result acknowledgement when test isolation result handler returns DoNotTransitionButSaveTestResult`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = Default()
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResult(
                currentState,
                testResult
            )
        } returns DoNotTransitionButStoreTestResult(testResultStorageOperation = Overwrite)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = currentState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult, testResultStorageOperation = Overwrite), sideEffect)
        verify(exactly = 1) { testResultHandler.acknowledge(testResult, expected.symptomsOnsetDate, testResultStorageOperation = Overwrite) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `do not transition from isolation on test result acknowledgement when test isolation result handler returns DoNotTransitionButSaveTestResult`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val currentState = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(10)
            )
        )
        every { stateProvider.state } returns currentState
        every {
            testResultIsolationHandler.computeTransitionWithTestResult(
                currentState,
                testResult
            )
        } returns DoNotTransitionButStoreTestResult(testResultStorageOperation = Overwrite)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = currentState
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult, testResultStorageOperation = Overwrite), sideEffect)
        verify(exactly = 1) { testResultHandler.acknowledge(testResult, expected.symptomsOnsetDate, testResultStorageOperation = Overwrite) }
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }

        newStateIsolationChecks(
            currentState = currentState,
            newState = currentState
        )
    }

    @Test
    fun `verify expiration from contact case to default state`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock).minusDays(1)
        val contactCase = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDate,
                notificationDate = null,
                expiryDate = expiryDate
            )
        )

        every { stateProvider.state } returns contactCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val actual = testSubject.readState()

        assertEquals(Default(previousIsolation = contactCase), actual)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `verify expiration from index case to default state`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock).minusDays(1)
        val indexCase = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-05-20"),
                expiryDate = expiryDate,
                selfAssessment = false
            )
        )

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val actual = testSubject.readState()

        assertEquals(Default(previousIsolation = indexCase), actual)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `verify expiration from previous isolation to default state`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock)
            .minusDays(durationDays.pendingTasksRetentionPeriod.toLong())
            .minusDays(1)
        val default = Default(
            previousIsolation = Isolation(
                isolationStart = startDate,
                isolationConfiguration = durationDays,
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.parse("2020-05-20"),
                    expiryDate = expiryDate,
                    selfAssessment = false
                )
            )
        )

        every { stateProvider.state } returns default

        val testSubject = createIsolationStateMachine(fixedClock)

        val actual = testSubject.readState()

        assertEquals(Default(), actual)
        verify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `reset should put state machine in default state with no history`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val indexCase = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-05-20"),
                expiryDate = expiryDate,
                selfAssessment = false
            )
        )

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        testSubject.reset()

        assertEquals(Default(), testSubject.readState())
        coVerify(exactly = 0) { analyticsEventTracker.track(StartedIsolation) }
        verify { alarmController.cancelExpirationCheckIfAny() }
        verify(exactly = 1) { exposureNotificationHandler.cancel() }
    }

    @Test
    fun `isolation state expiry date`() {
        val expiryDateDayBeforeYesterday = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).minusDays(2)
            )
        )

        val expiryDateYesterday = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        val expiryDateToday = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock)
            )
        )

        assertTrue(expiryDateDayBeforeYesterday.hasExpired(fixedClock, daysAgo = 1))
        assertTrue(expiryDateYesterday.hasExpired(fixedClock, daysAgo = 1))
        assertFalse(expiryDateToday.hasExpired(fixedClock, daysAgo = 1))
    }

    @Test
    fun `remaining days in isolation is 0 when state is Default`() {
        val testSubject = createIsolationStateMachine(fixedClock)
        val state = Default()

        val remainingDaysInIsolation = testSubject.remainingDaysInIsolation(state)

        assertEquals(0, remainingDaysInIsolation)
    }

    @Test
    fun `remaining days in isolation is number of days until isolation end when in isolation`() {
        val testSubject = createIsolationStateMachine(fixedClock)
        val state = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(14)
            )
        )

        val remainingDaysInIsolation = testSubject.remainingDaysInIsolation(state)

        assertEquals(14, remainingDaysInIsolation)
    }

    @Test
    fun `testBelongsToIsolation returns false when test before isolation start`() {
        val state = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(14)
            )
        )

        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock).minus(1, ChronoUnit.DAYS),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val result = state.testBelongsToIsolation(testResult)
        assertFalse(result)
    }

    @Test
    fun `testBelongsToIsolation returns true when test equals isolation start`() {
        val state = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(14)
            )
        )

        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val result = state.testBelongsToIsolation(testResult)
        assertTrue(result)
    }

    @Test
    fun `testBelongsToIsolation returns true when test after isolation start`() {
        val state = Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(14)
            )
        )

        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock).plus(1, ChronoUnit.DAYS),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            diagnosisKeySubmissionSupported = true,
            requiresConfirmatoryTest = false
        )

        val result = state.testBelongsToIsolation(testResult)
        assertTrue(result)
    }

    @Test
    fun `when in contact case only and opt-in to daily contact testing, transition to default and store contact case isolation and opt-in date`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(3))
        val notificationDate = Instant.now(fixedClock).minus(Duration.ofDays(2))
        val expiryDate = LocalDate.now(fixedClock).plusDays(12)
        val contactCaseOnlyIsolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDate,
                notificationDate = notificationDate,
                expiryDate = expiryDate,
                dailyContactTestingOptInDate = null
            )
        )

        every { stateProvider.state } returns contactCaseOnlyIsolation

        val testSubject = createIsolationStateMachine(fixedClock)

        testSubject.optInToDailyContactTesting()

        val actual = testSubject.readState()
        val expected = Default(
            previousIsolation = contactCaseOnlyIsolation.copy(
                contactCase = contactCaseOnlyIsolation.contactCase!!.copy(
                    expiryDate = LocalDate.now(fixedClock),
                    dailyContactTestingOptInDate = LocalDate.now(fixedClock)
                )
            )
        )

        verify(exactly = 1) { analyticsEventTracker.track(DeclaredNegativeResultFromDct) }

        assertEquals(expected, actual)
    }

    @Test
    fun `when other than contact case only and opt-in to daily contact testing do not transition`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(3))
        val notificationDate = Instant.now(fixedClock).minus(Duration.ofDays(2))
        val expiryDate = LocalDate.now(fixedClock).plusDays(12)
        val combinedIsolation = Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-05-20"),
                expiryDate = expiryDate.minusDays(3),
                selfAssessment = false
            ),
            contactCase = ContactCase(
                startDate = startDate,
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

    private fun newStateIsolationChecks(currentState: State, newState: State) {
        val newIsolationState = newState as Isolation
        verify { alarmController.setupExpirationCheck(currentState, newIsolationState) }
    }

    private fun isolationStateWithIndexCase(expiryDaysFromStartDate: Long, selfAssessment: Boolean): Isolation {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(expiryDaysFromStartDate)
        return Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-05-18"),
                expiryDate = expiryDate,
                selfAssessment = selfAssessment
            )
        )
    }

    private fun isolationStateWithContactCase(expiryDaysFromStartDate: Long): Isolation {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(expiryDaysFromStartDate)
        return Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDate,
                notificationDate = startDate,
                expiryDate = expiryDate
            )
        )
    }

    private fun createIsolationStateMachine(clock: Clock): IsolationStateMachine {
        return IsolationStateMachine(
            stateProvider,
            notificationProvider,
            isolationConfigurationProvider,
            relevantTestResultProvider,
            testResultHandler,
            testResultIsolationHandler,
            userInbox,
            alarmController,
            clock,
            analyticsEventTracker,
            exposureNotificationHandler
        )
    }
}
