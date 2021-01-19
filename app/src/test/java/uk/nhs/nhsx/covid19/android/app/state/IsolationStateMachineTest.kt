package uk.nhs.nhsx.covid19.android.app.state

import com.tinder.StateMachine.Transition
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.notifications.AddableUserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.AcknowledgeTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.ClearAcknowledgedTestResults
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.HandleTestResult
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
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
    private val isolationConfigurationProvider =
        mockk<IsolationConfigurationProvider>(relaxed = true)
    private val testResultProvider = mockk<TestResultsProvider>(relaxed = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val testScope = TestCoroutineScope()

    private val durationDays =
        DurationDays(
            contactCase = 14,
            indexCaseSinceSelfDiagnosisOnset = 5,
            indexCaseSinceSelfDiagnosisUnknownOnset = 3,
            maxIsolation = 21,
            pendingTasksRetentionPeriod = 14
        )

    @Before
    fun setUp() = testScope.runBlockingTest {
        every { isolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `from default to index case on positive self assessment and user cannot remember onset date`() = testScope.runBlockingTest {
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
            startDateForSymptomatic,
            durationDays,
            indexCase = IndexCase(
                LocalDate.parse("2020-05-19"),
                expiryDateForSymptomatic,
                true
            )
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(ClearAcknowledgedTestResults, sideEffect)
        coVerify(exactly = 1) { analyticsEventProcessor.track(StartedIsolation) }
        verify(exactly = 1) { testResultProvider.clearAcknowledged() }

        newStateIsolationChecks(actualState)
    }

    @Test
    fun `stay in default state on positive self assessment and onset date is too long ago`() = testScope.runBlockingTest {
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
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        verify(exactly = 0) { testResultProvider.clearAcknowledged() }

        newStateDefaultChecks()
    }

    @Test
    fun `from default to index state on positive self assessment and onset date is recent`() = testScope.runBlockingTest {
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
        assertEquals(ClearAcknowledgedTestResults, sideEffect)
        coVerify(exactly = 1) { analyticsEventProcessor.track(StartedIsolation) }
        verify(exactly = 1) { testResultProvider.clearAcknowledged() }

        newStateIsolationChecks(actualState)
    }

    @Test
    fun `stay in default on positive test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
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
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `stay in default on positive test result when previously in index case acknowledgement`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult
        every { testResultProvider.isLastRelevantTestResultPositive() } returns true
        every { testResultProvider.isLastRelevantTestResultNegative() } returns false
        every { stateProvider.state } returns Default(previousIsolation = isolationStateWithIndexCase(3, false))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = Default(previousIsolation = isolationStateWithIndexCase(3, false))
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `start isolation on positive test result when previously not in index case acknowledge`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult
        every { testResultProvider.isLastRelevantTestResultPositive() } returns true
        every { testResultProvider.isLastRelevantTestResultNegative() } returns false
        every { stateProvider.state } returns Default()

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = Isolation(
            Instant.now(fixedClock),
            durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(3),
                expiryDate = LocalDate.now(fixedClock).plusDays(11),
                selfAssessment = false
            )
        )
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 1) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay in default on void test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
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
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `stay in default on void test result acknowledgement`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        every { stateProvider.state } returns Default()

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(Default(), actual)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `from default to contact case on exposed notification contact 5 days ago`() = testScope.runBlockingTest {
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
        verify(exactly = 1) { notificationProvider.showExposureNotification() }
        verify(exactly = 1) { userInbox.addUserInboxItem(ShowEncounterDetection) }
        coVerify(exactly = 1) { analyticsEventProcessor.track(StartedIsolation) }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyContactNotification) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay default on exposed notification contact 14 days ago`() = testScope.runBlockingTest {
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
        verify(exactly = 0) { notificationProvider.showExposureNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowEncounterDetection) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyContactNotification) }

        newStateDefaultChecks()
    }

    @Test
    fun `stay default on exposed notification contact 15 days ago`() = testScope.runBlockingTest {
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
        verify(exactly = 0) { notificationProvider.showExposureNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowEncounterDetection) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyContactNotification) }

        newStateDefaultChecks()
    }

    @Test
    fun `in index case on exposed notification add contact case`() = testScope.runBlockingTest {
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
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        coVerify(exactly = 1) { analyticsEventProcessor.track(ReceivedRiskyContactNotification) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `in contact case on exposed notification add contact case`() = testScope.runBlockingTest {
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
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(ReceivedRiskyContactNotification) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay in index case on exposed notification with positive test`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            LocalDate.parse("2020-05-25").atStartOfDay().toInstant(ZoneOffset.UTC),
            POSITIVE
        )
        every { testResultProvider.testResults } returns mapOf(testResult.diagnosisKeySubmissionToken to testResult)

        val initialState = isolationStateWithIndexCase(3, false)
        every { stateProvider.state } returns isolationStateWithIndexCase(3, false)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(initialState, actual)
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(ReceivedRiskyContactNotification) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay in index case on positive test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
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
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay in index case without self-assessment on positive test result acknowledgement`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val state = isolationStateWithIndexCase(3, false)
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        val expected = state.copy()
        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay in index case with self-assessment on positive test result acknowledgement`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE
        )

        val state = isolationStateWithIndexCase(3, true)
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        val expected = state.copy()
        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay in isolation on negative test result and positive previous test result acknowledge`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "token2",
            testEndDate = Instant.now(fixedClock),
            testResult = NEGATIVE
        )

        every { stateProvider.state } returns Isolation(
            Instant.now(fixedClock),
            durationDays
        )
        every { testResultProvider.isLastRelevantTestResultPositive() } returns true
        every { testResultProvider.isLastRelevantTestResultNegative() } returns false

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult, true)
        )

        val newState = testSubject.readState()

        val expected = Isolation(
            Instant.now(fixedClock),
            durationDays
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(testResult, true), sideEffect)
        verify(exactly = 0) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 1) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `start isolation on positive test result and negative previous test result acknowledge`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "token2",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE
        )

        every { stateProvider.state } returns Default()
        every { testResultProvider.isLastRelevantTestResultPositive() } returns false
        every { testResultProvider.isLastRelevantTestResultNegative() } returns true

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val expected = isolationStateWithIndexCase(11, false)

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 1) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `stay in index case on void test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
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
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay in index case on void test result acknowledgement`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val state = isolationStateWithIndexCase(3, false)
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        val expected = state.copy()
        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `stay in index case to default on negative test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(5)
        val isolation = Isolation(
            startDate,
            durationDays,
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
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `from index case to default on negative test result acknowledgement`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(5)
        val isolation = Isolation(
            startDate,
            durationDays,
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.parse("2020-05-20"),
                expiryDate = expiryDate,
                selfAssessment = false
            )
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val sideEffect = (transition as Transition.Valid).sideEffect

        val previousIsolation = isolation.copy()
        assertEquals(Default(previousIsolation), newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `index case with previous contact case stays index case on negative test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )

        val state = Isolation(
            Instant.now(fixedClock),
            durationDays,
            contactCase = ContactCase(Instant.now(fixedClock), null, LocalDate.parse("2020-05-22")),
            indexCase = IndexCase(LocalDate.parse("2020-05-20"), LocalDate.parse("2020-05-22"), false)
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
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `index case with previous contact case stays index case on negative test result acknowledge`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val state = Isolation(
            Instant.now(fixedClock),
            durationDays,
            contactCase = ContactCase(Instant.now(fixedClock), null, LocalDate.parse("2020-05-21")),
            indexCase = IndexCase(LocalDate.parse("2020-05-20"), LocalDate.parse("2020-05-22"), false)
        )
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val sideEffect = (transition as Transition.Valid).sideEffect

        val expectedState = Default(previousIsolation = state.copy(indexCase = null))
        assertEquals(expectedState, newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `from contact case to index case on positive self assessment and contact case finishes later than index`() = testScope.runBlockingTest {
        val contactCaseStartDate = Instant.now(fixedClock)
        val contactCaseExpiryDate = LocalDate.now(fixedClock).plusDays(365)
        val contactCase = ContactCase(contactCaseStartDate, null, contactCaseExpiryDate)
        every { stateProvider.state } returns Isolation(
            contactCaseStartDate,
            durationDays,
            contactCase = contactCase
        )

        val testSubject = createIsolationStateMachine(fixedClock)

        val startDate = LocalDate.parse("2020-05-19")
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(SelectedDate.ExplicitDate(startDate))
        )

        val actual = testSubject.readState()

        val startDateForIndex = Instant.now(fixedClock)
        val expected =
            Isolation(
                startDateForIndex,
                durationDays,
                indexCase = IndexCase(startDate, startDate.plusDays(23), true),
                contactCase = contactCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(ClearAcknowledgedTestResults, sideEffect)
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        verify(exactly = 1) { testResultProvider.clearAcknowledged() }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `from contact case to index case on positive self assessment and user cannot remember onset date`() = testScope.runBlockingTest {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val contactCase = ContactCase(startDate, null, expiryDate)
        every { stateProvider.state } returns Isolation(
            startDate,
            durationDays,
            contactCase = contactCase
        )

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
                startDateForIndex,
                durationDays,
                indexCase = IndexCase(LocalDate.parse("2020-05-19"), expiryDateForIndex, true),
                contactCase = contactCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(ClearAcknowledgedTestResults, sideEffect)
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        verify(exactly = 1) { testResultProvider.clearAcknowledged() }

        newStateIsolationChecks(actual)
    }

    // Note: this shouldn't actually happen since it's not possible to perform a self-assessment when already in isolation as an index case
    @Test
    fun `from index case with positive test to index case with self assessment on positive self assessment and user cannot remember onset date`() = testScope.runBlockingTest {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val indexCase = IndexCase(LocalDate.parse("2020-05-19"), expiryDate, false)
        every { stateProvider.state } returns Isolation(
            startDate,
            durationDays,
            indexCase
        )

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
                startDateForIndex,
                durationDays,
                indexCase = IndexCase(LocalDate.parse("2020-05-19"), expiryDateForIndex, true)
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }
        verify(exactly = 0) { testResultProvider.clearAcknowledged() }

        newStateIsolationChecks(actual)
    }

    @Test
    fun `keep contact case on positive test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns Isolation(
            startDate,
            durationDays,
            contactCase = ContactCase(startDate, null, expiryDate)
        )

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected,
            durationDays,
            contactCase = ContactCase(startDateExpected, null, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `keep contact case and add index case on recent positive test result acknowledge`() = testScope.runBlockingTest {
        val testEndInstant = Instant.now(fixedClock).minus(2, ChronoUnit.DAYS)
        val testEndDate = testEndInstant.atZone(fixedClock.zone).toLocalDate()
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = testEndInstant,
            testResult = POSITIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns Isolation(
            startDate,
            durationDays,
            contactCase = ContactCase(startDate, null, expiryDate)
        )

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateContactExpected = LocalDate.now(fixedClock).plusDays(6)
        val expiryDateIndexExpected = testEndDate.plusDays(11)
        val expected = Isolation(
            startDateExpected,
            durationDays,
            contactCase = ContactCase(
                startDate = startDateExpected,
                notificationDate = null,
                expiryDate = expiryDateContactExpected
            ),
            indexCase = IndexCase(
                symptomsOnsetDate = testEndDate.minusDays(3),
                expiryDate = expiryDateIndexExpected,
                selfAssessment = false
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(expiryDateIndexExpected, (newState as Isolation).expiryDate)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `keep contact case and add index case on older positive test result acknowledge`() = testScope.runBlockingTest {
        val testEndInstant = Instant.now(fixedClock).minus(7, ChronoUnit.DAYS)
        val testEndDate = testEndInstant.atZone(fixedClock.zone).toLocalDate()
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "123",
            testEndDate = testEndInstant,
            testResult = POSITIVE
        )

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns Isolation(
            isolationStart = startDate,
            isolationConfiguration = durationDays,
            contactCase = ContactCase(
                startDate = startDate,
                notificationDate = null,
                expiryDate = expiryDate
            )
        )

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateContactExpected = LocalDate.now(fixedClock).plusDays(6)
        val expiryDateIndexExpected = testEndDate.plusDays(11)
        val expected = Isolation(
            startDateExpected,
            durationDays,
            contactCase = ContactCase(
                startDate = startDateExpected,
                notificationDate = null,
                expiryDate = expiryDateContactExpected
            ),
            indexCase = IndexCase(
                symptomsOnsetDate = testEndDate.minusDays(3),
                expiryDate = expiryDateIndexExpected,
                selfAssessment = false
            )
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(expiryDateContactExpected, (newState as Isolation).expiryDate)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `keep contact case on negative test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, null, expiryDate))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, durationDays,
            contactCase = ContactCase(startDateExpected, null, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `keep contact case on negative test result acknowledgement`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, null, expiryDate))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, durationDays,
            contactCase = ContactCase(startDateExpected, null, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `keep contact case on void test result`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
        )

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, null, expiryDate))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, durationDays,
            contactCase = ContactCase(startDateExpected, null, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultProvider.add(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `keep contact case on void test result acknowledgement`() = testScope.runBlockingTest {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, null, expiryDate))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, durationDays,
            contactCase = ContactCase(startDateExpected, null, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateIsolationChecks(newState)
    }

    @Test
    fun `stay in default state when isolation triggered by positive test result expired`() = testScope.runBlockingTest {
        val oldTestResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock).minus(11, ChronoUnit.DAYS),
            POSITIVE
        )
        every { testResultProvider.find(oldTestResult.diagnosisKeySubmissionToken) } returns oldTestResult

        every { stateProvider.state } returns Default()

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(OnTestResultAcknowledge(oldTestResult))

        val newState = testSubject.readState()

        val expected = Default()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(oldTestResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(oldTestResult) }
        verify(exactly = 0) { testResultProvider.remove(oldTestResult) }
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `verify expiration from contact case to default state`() = testScope.runBlockingTest {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock).minusDays(1)
        val contactCase =
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, null, expiryDate))

        every { stateProvider.state } returns contactCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val actual = testSubject.readState()

        assertEquals(Default(previousIsolation = contactCase), actual)
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `verify expiration from index case to default state`() = testScope.runBlockingTest {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock).minusDays(1)
        val indexCase =
            Isolation(startDate, durationDays, IndexCase(LocalDate.parse("2020-05-20"), expiryDate, false))

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val actual = testSubject.readState()

        assertEquals(Default(previousIsolation = indexCase), actual)
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `verify expiration from previous isolation to default state`() = testScope.runBlockingTest {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock)
            .minusDays(durationDays.pendingTasksRetentionPeriod.toLong())
            .minusDays(1)
        val default =
            Default(
                previousIsolation = Isolation(
                    startDate,
                    durationDays,
                    IndexCase(LocalDate.parse("2020-05-20"), expiryDate, false)
                )
            )

        every { stateProvider.state } returns default

        val testSubject = createIsolationStateMachine(fixedClock)

        val actual = testSubject.readState()

        assertEquals(Default(), actual)
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
    }

    @Test
    fun `reset should put state machine in default state with no history`() = testScope.runBlockingTest {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val indexCase =
            Isolation(startDate, durationDays, IndexCase(LocalDate.parse("2020-05-20"), expiryDate, false))

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        testSubject.reset()

        assertEquals(Default(), testSubject.readState())
        coVerify(exactly = 0) { analyticsEventProcessor.track(StartedIsolation) }

        newStateDefaultChecks()
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

    private fun newStateDefaultChecks() = testScope.runBlockingTest {
        verify { alarmController.cancelExpirationCheckIfAny() }

        verify { notificationProvider.cancelExposureNotification() }
        verify { userInbox.clearItem(ShowEncounterDetection) }
    }

    private fun newStateIsolationChecks(newState: State) {
        val newIsolationState = newState as Isolation
        verify { alarmController.setupExpirationCheck(newIsolationState.expiryDate) }
    }

    private fun isolationStateWithIndexCase(expiryDaysFromStartDate: Long, selfAssessment: Boolean): Isolation {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(expiryDaysFromStartDate)
        return Isolation(
            startDate, durationDays,
            indexCase = IndexCase(LocalDate.parse("2020-05-18"), expiryDate, selfAssessment)
        )
    }

    private fun isolationStateWithContactCase(expiryDaysFromStartDate: Long): Isolation {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(expiryDaysFromStartDate)
        return Isolation(
            startDate, durationDays,
            contactCase = ContactCase(startDate, startDate, expiryDate)
        )
    }

    private fun createIsolationStateMachine(clock: Clock): IsolationStateMachine {
        return IsolationStateMachine(
            stateProvider,
            notificationProvider,
            isolationConfigurationProvider,
            testResultProvider,
            userInbox,
            alarmController,
            clock,
            analyticsEventProcessor,
            testScope
        )
    }
}
