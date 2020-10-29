package uk.nhs.nhsx.covid19.android.app.state

import com.tinder.StateMachine.Transition
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
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
import kotlin.test.assertNull

class IsolationStateMachineTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val stateProvider = mockk<StateStorage>(relaxed = true)
    private val notificationProvider = mockk<NotificationProvider>(relaxed = true)
    private val userInbox = mockk<UserInbox>(relaxed = true)
    private val alarmController = mockk<IsolationExpirationAlarmController>(relaxed = true)
    private val isolationConfigurationProvider =
        mockk<IsolationConfigurationProvider>(relaxed = true)
    private val testResultProvider = mockk<TestResultsProvider>(relaxed = true)

    private val durationDays =
        DurationDays(
            contactCase = 14,
            indexCaseSinceSelfDiagnosisOnset = 5,
            indexCaseSinceSelfDiagnosisUnknownOnset = 3,
            maxIsolation = 21
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
            startDateForSymptomatic,
            durationDays,
            indexCase = IndexCase(
                LocalDate.parse("2020-05-19"),
                expiryDateForSymptomatic
            )
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
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
                onsetDate,
                expiryDate = expiryDateForSymptomatic
            ),
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = durationDays
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
    }

    @Test
    fun `stay in default on positive test result`() {
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
    }

    @Test
    fun `stay in default on positive test result when previously in index case acknowledgement`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult
        every { testResultProvider.isLastTestResultPositive() } returns true
        every { testResultProvider.isLastTestResultNegative() } returns false
        every { stateProvider.state } returns Default(previousIsolation = isolationStateWithIndexCase(3))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val actual = testSubject.readState()

        val expected = Default(previousIsolation = isolationStateWithIndexCase(3))
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
    }

    @Test
    fun `start isolation on positive test result when previously not in index case acknowledge`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult
        every { testResultProvider.isLastTestResultPositive() } returns true
        every { testResultProvider.isLastTestResultNegative() } returns false
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
                LocalDate.now(fixedClock).minusDays(3),
                LocalDate.now(fixedClock).plusDays(10)
            )
        )
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
    }

    @Test
    fun `stay in default on void test result`() {
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
    }

    @Test
    fun `stay in default on void test result acknowledgement`() {
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

        val startDate = Instant.now(today)
        val expiryDate = exposureDate.atZone(ZoneOffset.UTC).toLocalDate()
            .plusDays(durationDays.contactCase.toLong())
        val expected =
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, expiryDate))

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SendExposedNotification, sideEffect)
        verify(exactly = 1) { notificationProvider.showExposureNotification() }
        verify(exactly = 1) { userInbox.addUserInboxItem(ShowEncounterDetection) }
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
        verify(exactly = 0) { notificationProvider.showExposureNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowEncounterDetection) }
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
        verify(exactly = 0) { notificationProvider.showExposureNotification() }
        verify(exactly = 0) { userInbox.addUserInboxItem(ShowEncounterDetection) }
    }

    @Test
    fun `in index case on exposed notification add contact case`() {
        val case = isolationStateWithIndexCase(3)
        every { stateProvider.state } returns isolationStateWithIndexCase(3)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(
            case.copy(
                contactCase = ContactCase(
                    Instant.now(fixedClock),
                    expiryDate = LocalDate.now(fixedClock)
                        .plusDays(durationDays.contactCase.toLong())
                )
            ),
            actual
        )
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(SendExposedNotification, sideEffect)
    }

    @Test
    fun `stay in index case on exposed notification with positive test`() {
        val testResult = ReceivedTestResult(
            "123",
            LocalDate.parse("2020-05-25").atStartOfDay().toInstant(ZoneOffset.UTC),
            POSITIVE
        )
        every { testResultProvider.testResults } returns mapOf(testResult.diagnosisKeySubmissionToken to testResult)

        val initialState = isolationStateWithIndexCase(3)
        every { stateProvider.state } returns isolationStateWithIndexCase(3)

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(initialState, actual)
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
    }

    @Test
    fun `stay in index case on positive test result`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )

        val state = isolationStateWithIndexCase(3)
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
    }

    @Test
    fun `stay in index case on positive test result acknowledgement`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val state = isolationStateWithIndexCase(3)
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
    }

    @Test
    fun `stay in isolation on negative test result and positive previous test result acknowledge`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "token2",
            testEndDate = Instant.now(fixedClock),
            testResult = NEGATIVE
        )

        every { stateProvider.state } returns Isolation(
            Instant.now(fixedClock),
            durationDays
        )
        every { testResultProvider.isLastTestResultPositive() } returns true
        every { testResultProvider.isLastTestResultNegative() } returns false

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
    }

    @Test
    fun `start isolation on positive test result and negative previous test result acknowledge`() {
        val testResult = ReceivedTestResult(
            diagnosisKeySubmissionToken = "token2",
            testEndDate = Instant.now(fixedClock),
            testResult = POSITIVE
        )

        every { stateProvider.state } returns Default()
        every { testResultProvider.isLastTestResultPositive() } returns false
        every { testResultProvider.isLastTestResultNegative() } returns true

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val expected = isolationStateWithIndexCase(10)

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
    }

    @Test
    fun `stay in index case on void test result`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
        )

        val state = isolationStateWithIndexCase(3)
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
    }

    @Test
    fun `stay in index case on void test result acknowledgement`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val state = isolationStateWithIndexCase(3)
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
    }

    @Test
    fun `stay in index case to default on negative test result`() {
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
                LocalDate.parse("2020-05-20"),
                expiryDate
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
    }

    @Test
    fun `from index case to default on negative test result acknowledgement`() {
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
                LocalDate.parse("2020-05-20"),
                expiryDate
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
    }

    @Test
    fun `index case with previous contact case stays index case on negative test result`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )

        val state = Isolation(
            Instant.now(fixedClock),
            durationDays,
            contactCase = ContactCase(Instant.now(fixedClock), LocalDate.parse("2020-05-22")),
            indexCase = IndexCase(LocalDate.parse("2020-05-20"), LocalDate.parse("2020-05-22"))
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
    }

    @Test
    fun `index case with previous contact case stays index case on negative test result acknowledge`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val state = Isolation(
            Instant.now(fixedClock),
            durationDays,
            contactCase = ContactCase(Instant.now(fixedClock), LocalDate.parse("2020-05-21")),
            indexCase = IndexCase(LocalDate.parse("2020-05-20"), LocalDate.parse("2020-05-22"))
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
    }

    @Test
    fun `from contact case to index case on positive self assessment and contact case finishes later than index`() {
        val contactCaseStartDate = Instant.now(fixedClock)
        val contactCaseExpiryDate = LocalDate.now(fixedClock).plusDays(365)
        val contactCase = ContactCase(contactCaseStartDate, contactCaseExpiryDate)
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
                indexCase = IndexCase(startDate, startDate.plusDays(23)),
                contactCase = contactCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
    }

    @Test
    fun `from contact case to index case on positive self assessment and user cannot remember onset date`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val contactCase = ContactCase(startDate, expiryDate)
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
                indexCase = IndexCase(LocalDate.parse("2020-05-19"), expiryDateForIndex),
                contactCase = contactCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
    }

    @Test
    fun `keep contact case on positive test result`() {
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
            contactCase = ContactCase(startDate, expiryDate)
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
            contactCase = ContactCase(startDateExpected, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultProvider.add(testResult) }
    }

    @Test
    fun `keep contact case on positive test result acknowledge`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            POSITIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns Isolation(
            startDate,
            durationDays,
            contactCase = ContactCase(startDate, expiryDate)
        )

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected,
            durationDays,
            contactCase = ContactCase(startDateExpected, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
    }

    @Test
    fun `keep contact case on negative test result`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, expiryDate))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, durationDays,
            contactCase = ContactCase(startDateExpected, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultProvider.add(testResult) }
    }

    @Test
    fun `keep contact case on negative test result acknowledgement`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            NEGATIVE
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, expiryDate))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, durationDays,
            contactCase = ContactCase(startDateExpected, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
    }

    @Test
    fun `keep contact case on void test result`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
        )

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, expiryDate))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResult(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, durationDays,
            contactCase = ContactCase(startDateExpected, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(HandleTestResult(testResult), sideEffect)
        verify { testResultProvider.add(testResult) }
    }

    @Test
    fun `keep contact case on void test result acknowledgement`() {
        val testResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock),
            VOID
        )
        every { testResultProvider.find(testResult.diagnosisKeySubmissionToken) } returns testResult

        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, expiryDate))

        val testSubject = createIsolationStateMachine(fixedClock)

        val transition = testSubject.processEvent(
            OnTestResultAcknowledge(testResult)
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, durationDays,
            contactCase = ContactCase(startDateExpected, expiryDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(AcknowledgeTestResult(testResult), sideEffect)
        verify(exactly = 1) { testResultProvider.acknowledge(testResult) }
        verify(exactly = 0) { testResultProvider.remove(testResult) }
    }

    @Test
    fun `stay in default state when isolation triggered by positive test result expired`() {
        val oldTestResult = ReceivedTestResult(
            "123",
            Instant.now(fixedClock).minus(10, ChronoUnit.DAYS),
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
    }

    @Test
    fun `verify expiration from contact case to default state`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock).minusDays(1)
        val contactCase =
            Isolation(startDate, durationDays, contactCase = ContactCase(startDate, expiryDate))

        every { stateProvider.state } returns contactCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val actual = testSubject.readState()

        assertEquals(Default(previousIsolation = contactCase), actual)
    }

    @Test
    fun `verify expiration from index case to default state`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock).minusDays(1)
        val indexCase =
            Isolation(startDate, durationDays, IndexCase(LocalDate.parse("2020-05-20"), expiryDate))

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        val actual = testSubject.readState()

        assertEquals(Default(previousIsolation = indexCase), actual)
    }

    @Test
    fun `reset should put state machine in default state with no history`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val indexCase =
            Isolation(startDate, durationDays, IndexCase(LocalDate.parse("2020-05-20"), expiryDate))

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine(fixedClock)

        testSubject.reset()

        assertEquals(Default(), testSubject.readState())
    }

    private fun isolationStateWithIndexCase(expiryDaysFromStartDate: Long): Isolation {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(expiryDaysFromStartDate)
        return Isolation(
            startDate, durationDays,
            indexCase = IndexCase(LocalDate.parse("2020-05-18"), expiryDate)
        )
    }

    private fun createIsolationStateMachine(clock: Clock): IsolationStateMachine {
        return IsolationStateMachine(
            stateProvider,
            notificationProvider,
            clock,
            isolationConfigurationProvider,
            testResultProvider,
            userInbox,
            alarmController
        )
    }
}
