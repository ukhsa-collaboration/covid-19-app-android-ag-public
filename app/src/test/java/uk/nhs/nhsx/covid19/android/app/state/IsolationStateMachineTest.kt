package uk.nhs.nhsx.covid19.android.app.state

import com.tinder.StateMachine.Transition
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.UserInbox
import uk.nhs.nhsx.covid19.android.app.notifications.UserInboxItem.ShowEncounterDetection
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendExposedNotification
import uk.nhs.nhsx.covid19.android.app.state.SideEffect.SendTestResultNotification
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
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

        val testSubject = createIsolationStateMachine()

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
            expiryDateForSymptomatic,
            indexCase = IndexCase(
                LocalDate.parse("2020-05-19")
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

        val testSubject = createIsolationStateMachine()

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

        val testSubject = createIsolationStateMachine()

        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(SelectedDate.ExplicitDate(onsetDate))
        )

        val actualState = testSubject.readState()

        val expiryDateForSymptomatic =
            onsetDate.plusDays(durationDays.indexCaseSinceSelfDiagnosisOnset.toLong() - daysBeforeToday + 1)
        val expectedState = Isolation(
            indexCase = IndexCase(
                onsetDate
            ),
            isolationStart = Instant.now(fixedClock),
            expiryDate = expiryDateForSymptomatic
        )
        assertEquals(expectedState, actualState)

        val sideEffect = (transition as Transition.Valid).sideEffect
        assertNull(sideEffect)
    }

    @Test
    fun `stay in default on positive test result`() {
        every { stateProvider.state } returns Default()

        val testSubject = createIsolationStateMachine()

        val transition = testSubject.processEvent(
            OnPositiveTestResult(
                Instant.now(fixedClock)
            )
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(Default(), actual)
        assertEquals(SendTestResultNotification, sideEffect)
    }

    @Test
    fun `from default to contact case on exposed notification`() {
        every { stateProvider.state } returns Default()

        val testSubject = createIsolationStateMachine()

        val exposureDate = Instant.parse("2020-05-15T10:00:00.00Z")
        val transition = testSubject.processEvent(
            OnExposedNotification(exposureDate)
        )

        val actual = testSubject.readState()

        val startDate = Instant.now(fixedClock)
        val expiryDate = exposureDate.atZone(ZoneOffset.UTC).toLocalDate().plusDays(durationDays.contactCase.toLong())
        val expected = Isolation(startDate, expiryDate, contactCase = ContactCase(startDate))

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertEquals(SendExposedNotification, sideEffect)
        verify(exactly = 1) { notificationProvider.showExposureNotification() }
        verify(exactly = 1) { userInbox.addUserInboxItem(ShowEncounterDetection) }
    }

    @Test
    fun `in index case on exposed notification add contact case`() {
        val case = indexCase()
        every { stateProvider.state } returns indexCase()

        val testSubject = createIsolationStateMachine()

        val transition = testSubject.processEvent(
            OnExposedNotification(Instant.now(fixedClock))
        )

        val actual = testSubject.readState()

        assertEquals(
            case.copy(
                expiryDate = LocalDate.now(fixedClock).plusDays(durationDays.contactCase.toLong()),
                contactCase = ContactCase(
                    Instant.now(fixedClock)
                )
            ),
            actual
        )
        val sideEffect = (transition as Transition.Valid).sideEffect
        assertEquals(SendExposedNotification, sideEffect)
    }

    @Test
    fun `stay in index case on positive test result`() {
        val state = indexCase()
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine()

        val testDate = Instant.now(fixedClock)
        val transition = testSubject.processEvent(
            OnPositiveTestResult(testDate)
        )

        val actual = testSubject.readState()
        val sideEffect = (transition as Transition.Valid).sideEffect

        val expected = state.copy(
            indexCase = state.indexCase?.copy(
                testResult = TestResult(
                    testDate,
                    POSITIVE
                )
            )
        )
        assertEquals(expected, actual)
        assertEquals(SendTestResultNotification, sideEffect)
    }

    @Test
    fun `from index case to default on negative test result`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(5)
        val isolation = Isolation(
            startDate,
            expiryDate,
            indexCase = IndexCase(
                LocalDate.parse("2020-05-20")
            )
        )
        every { stateProvider.state } returns isolation

        val testSubject = createIsolationStateMachine()

        val testDate = startDate.plus(Duration.ofDays(5))
        val transition = testSubject.processEvent(
            OnNegativeTestResult(testDate)
        )

        val newState = testSubject.readState()

        val sideEffect = (transition as Transition.Valid).sideEffect

        val previousIsolation = isolation.copy(
            indexCase = isolation.indexCase?.copy(
                testResult = TestResult(
                    testDate,
                    NEGATIVE
                )
            )
        )
        assertEquals(Default(previousIsolation), newState)
        assertEquals(SendTestResultNotification, sideEffect)
    }

    @Test
    fun `index case stays index case on negative test result if it was contact case before and test result is updated`() {
        val startDate = Instant.now(fixedClock)
        val state = Isolation(
            Instant.now(fixedClock),
            LocalDate.parse("2020-05-22"),
            contactCase = ContactCase(Instant.now(fixedClock)),
            indexCase = IndexCase(LocalDate.parse("2020-05-20"))
        )
        every { stateProvider.getHistory() } returns listOf(state)
        every { stateProvider.state } returns state

        val testSubject = createIsolationStateMachine()

        val testDate = startDate.plus(Duration.ofDays(5))
        val transition = testSubject.processEvent(
            OnNegativeTestResult(testDate)
        )

        val newState = testSubject.readState()

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(
            state.copy(
                indexCase = state.indexCase?.copy(
                    testResult = TestResult(
                        testDate,
                        NEGATIVE
                    )
                )
            ),
            newState
        )
        assertEquals(SendTestResultNotification, sideEffect)
    }

    @Test
    fun `from contact case to index case on positive self assessment and contact case finishes later than index`() {
        val contactCaseStartDate = Instant.now(fixedClock)
        val contactCaseExpiryDate = LocalDate.now(fixedClock).plusDays(365)
        val contactCase = ContactCase(contactCaseStartDate)
        every { stateProvider.state } returns Isolation(
            contactCaseStartDate,
            contactCaseExpiryDate,
            contactCase = contactCase
        )

        val testSubject = createIsolationStateMachine()

        val startDate = LocalDate.parse("2020-05-19")
        val transition = testSubject.processEvent(
            OnPositiveSelfAssessment(SelectedDate.ExplicitDate(startDate))
        )

        val actual = testSubject.readState()

        val startDateForIndex = Instant.now(fixedClock)
        val expected =
            Isolation(
                startDateForIndex,
                contactCaseExpiryDate,
                indexCase = IndexCase(startDate),
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
        val contactCase = ContactCase(startDate)
        every { stateProvider.state } returns Isolation(
            startDate,
            expiryDate,
            contactCase = contactCase
        )

        val testSubject = createIsolationStateMachine()

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
                expiryDateForIndex,
                indexCase = IndexCase(LocalDate.parse("2020-05-19")),
                contactCase = contactCase
            )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, actual)
        assertNull(sideEffect)
    }

    @Test
    fun `keep contact case on positive test result`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns Isolation(
            startDate,
            expiryDate,
            contactCase = ContactCase(startDate)
        )

        val testSubject = createIsolationStateMachine()

        val testDate = Instant.now(fixedClock)

        val transition = testSubject.processEvent(
            OnPositiveTestResult(
                testDate
            )
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected,
            expiryDateExpected,
            contactCase = ContactCase(startDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(SendTestResultNotification, sideEffect)
    }

    @Test
    fun `keep contact case on negative test result`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(6)

        every { stateProvider.state } returns
            Isolation(startDate, expiryDate, contactCase = ContactCase(startDate))

        val testSubject = createIsolationStateMachine()

        val testDate = Instant.now(fixedClock)

        val transition = testSubject.processEvent(
            OnNegativeTestResult(
                testDate
            )
        )

        val newState = testSubject.readState()

        val startDateExpected = Instant.now(fixedClock)
        val expiryDateExpected = LocalDate.now(fixedClock).plusDays(6)
        val expected = Isolation(
            startDateExpected, expiryDateExpected,
            contactCase = ContactCase(startDateExpected)
        )

        val sideEffect = (transition as Transition.Valid).sideEffect

        assertEquals(expected, newState)
        assertEquals(SendTestResultNotification, sideEffect)
    }

    @Test
    fun `verify expiration from contact case to default state`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock).minusDays(1)
        val contactCase = Isolation(startDate, expiryDate, contactCase = ContactCase(startDate))

        every { stateProvider.state } returns contactCase

        val testSubject = createIsolationStateMachine()

        val actual = testSubject.readState()

        assertEquals(Default(previousIsolation = contactCase), actual)
    }

    @Test
    fun `verify expiration from index case to default state`() {
        val startDate = Instant.now(fixedClock).minus(Duration.ofDays(15))
        val expiryDate = LocalDate.now(fixedClock).minusDays(1)
        val indexCase = Isolation(startDate, expiryDate, IndexCase(LocalDate.parse("2020-05-20")))

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine()

        val actual = testSubject.readState()

        assertEquals(Default(previousIsolation = indexCase), actual)
    }

    @Test
    fun `reset should put state machine in default state with no history`() {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        val indexCase = Isolation(startDate, expiryDate, IndexCase(LocalDate.parse("2020-05-20")))

        every { stateProvider.state } returns indexCase

        val testSubject = createIsolationStateMachine()

        testSubject.reset()

        assertEquals(Default(), testSubject.readState())
    }

    private fun indexCase(): Isolation {
        val startDate = Instant.now(fixedClock)
        val expiryDate = LocalDate.now(fixedClock).plusDays(3)
        return Isolation(
            startDate, expiryDate,
            indexCase = IndexCase(LocalDate.parse("2020-05-20"))
        )
    }

    private fun createIsolationStateMachine(): IsolationStateMachine {
        return IsolationStateMachine(
            stateProvider,
            notificationProvider,
            fixedClock,
            isolationConfigurationProvider,
            userInbox,
            alarmController
        )
    }
}
