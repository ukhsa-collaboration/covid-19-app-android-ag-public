package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveThenNegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.ViewState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class TestResultViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testResultsProvider = mockk<TestResultsProvider>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val fixedClock = Clock.fixed(symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)

    private val testSubject =
        TestResultViewModel(testResultsProvider, isolationConfigurationProvider, stateMachine, fixedClock)

    private val isolationState = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays()
    )

    private val isolationStateIndexCaseOnly = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(LocalDate.now(), expiryDate = symptomsOnsetDate.plus(7, ChronoUnit.DAYS), selfAssessment = false)
    )

    private val positiveTestResult = ReceivedTestResult(
        "token1", testEndDate = testEndDate, testResult = POSITIVE
    )
    private val positiveTestResultAcknowledged = ReceivedTestResult(
        "token2", testEndDate = testEndDate, testResult = POSITIVE, acknowledgedDate = Instant.now()
    )

    private val negativeTestResult = ReceivedTestResult(
        "token3", testEndDate = testEndDate, testResult = NEGATIVE
    )
    private val negativeTestResultAcknowledged = ReceivedTestResult(
        "token4", testEndDate = testEndDate, testResult = NEGATIVE, acknowledgedDate = Instant.now()
    )
    private val voidTestResult = ReceivedTestResult(
        "token5", testEndDate = testEndDate, testResult = VOID
    )
    private val voidTestResultAcknowledged = ReceivedTestResult(
        "token6", testEndDate = testEndDate, testResult = VOID, acknowledgedDate = Instant.now()
    )

    @Test
    fun `empty test results should return Ignore`() =
        runBlocking {
            every { stateMachine.readState() } returns Default()
            every { testResultsProvider.testResults } returns emptyMap()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(Ignore, 0)) }
        }

    @Test
    fun `latest test result positive and next in isolation should return PositiveWillBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                positiveTestResultAcknowledged.diagnosisKeySubmissionToken to positiveTestResultAcknowledged,
                positiveTestResult.diagnosisKeySubmissionToken to positiveTestResult
            )
            every { stateMachine.readState() } returns isolationState

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveContinueIsolation(positiveTestResult.diagnosisKeySubmissionToken), 0)
                )
            }
        }

    @Test
    fun `latest test result positive and next not in isolation should return PositiveWontBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                positiveTestResultAcknowledged.diagnosisKeySubmissionToken to positiveTestResultAcknowledged,
                positiveTestResult.diagnosisKeySubmissionToken to positiveTestResult
            )
            every { stateMachine.readState() } returns Default(previousIsolation = isolationStateIndexCaseOnly)

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation(positiveTestResult.diagnosisKeySubmissionToken), 0)
                )
            }
        }

    @Test
    fun `latest test result negative and currently not in isolation should return NegativeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                negativeTestResultAcknowledged.diagnosisKeySubmissionToken to negativeTestResultAcknowledged,
                negativeTestResult.diagnosisKeySubmissionToken to negativeTestResult
            )
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeNotInIsolation, 0)) }
        }

    @Test
    fun `latest test result negative and next in isolation should return NegativeWillBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                negativeTestResultAcknowledged.diagnosisKeySubmissionToken to negativeTestResultAcknowledged,
                negativeTestResult.diagnosisKeySubmissionToken to negativeTestResult
            )
            every { stateMachine.readState() } returns isolationState

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWillBeInIsolation, 0)) }
        }

    @Test
    fun `latest test result negative and next not in isolation should return NegativeWontBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                negativeTestResultAcknowledged.diagnosisKeySubmissionToken to negativeTestResultAcknowledged,
                negativeTestResult.diagnosisKeySubmissionToken to negativeTestResult
            )
            every { stateMachine.readState() } returns isolationStateIndexCaseOnly

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWontBeInIsolation, 0)) }
        }

    @Test
    fun `latest test result positive and then negative and next in isolation should return PositiveThenNegativeWillBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(negativeTestResult.diagnosisKeySubmissionToken to negativeTestResult)
            every { stateMachine.readState() } returns isolationState
            every { testResultsProvider.isLastTestResultPositive() } returns true

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(PositiveThenNegativeWillBeInIsolation, 0)) }
        }

    @Test
    fun `latest test result void and currently not in isolation should return VoidInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                voidTestResultAcknowledged.diagnosisKeySubmissionToken to voidTestResultAcknowledged,
                voidTestResult.diagnosisKeySubmissionToken to voidTestResult
            )
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidNotInIsolation, 0)) }
        }

    @Test
    fun `latest test result void and next in isolation should return VoidWillBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                voidTestResultAcknowledged.diagnosisKeySubmissionToken to voidTestResultAcknowledged,
                voidTestResult.diagnosisKeySubmissionToken to voidTestResult
            )
            every { stateMachine.readState() } returns isolationState

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidWillBeInIsolation, 0)) }
        }

    @Test
    fun `latest test results positive and void and next not in isolation should return PositiveWontBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                voidTestResult.diagnosisKeySubmissionToken to voidTestResult,
                positiveTestResult.diagnosisKeySubmissionToken to positiveTestResult
            )
            every { stateMachine.readState() } returns Default(previousIsolation = isolationStateIndexCaseOnly)

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation(positiveTestResult.diagnosisKeySubmissionToken), 0)
                )
            }
        }

    @Test
    fun `latest test results positive and void and next in isolation should return PositiveWillBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                voidTestResult.diagnosisKeySubmissionToken to voidTestResult,
                positiveTestResult.diagnosisKeySubmissionToken to positiveTestResult
            )
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolation(positiveTestResult.diagnosisKeySubmissionToken), 0)
                )
            }
        }

    @Test
    fun `latest test results positive and negative and next not in isolation should return PositiveWontBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                negativeTestResult.diagnosisKeySubmissionToken to negativeTestResult,
                positiveTestResult.diagnosisKeySubmissionToken to positiveTestResult
            )
            every { stateMachine.readState() } returns Default(previousIsolation = isolationStateIndexCaseOnly)

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation(positiveTestResult.diagnosisKeySubmissionToken), 0)
                )
            }
        }

    @Test
    fun `latest test results positive and negative and next in isolation should return PositiveWillBeInIsolation`() =
        runBlocking {
            every { testResultsProvider.testResults } returns mapOf(
                negativeTestResult.diagnosisKeySubmissionToken to negativeTestResult,
                positiveTestResult.diagnosisKeySubmissionToken to positiveTestResult
            )
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolation(positiveTestResult.diagnosisKeySubmissionToken), 0)
                )
            }
        }

    @Test
    fun `acknowledge test result should deliver event to state machine with remove flag`() {
        every { testResultsProvider.testResults } returns mapOf(negativeTestResult.diagnosisKeySubmissionToken to negativeTestResult)
        every { stateMachine.readState() } returns isolationState
        every { testResultsProvider.isLastTestResultPositive() } returns true

        testSubject.onCreate()

        testSubject.acknowledgeTestResult()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(negativeTestResult, true)) }
    }

    @Test
    fun `acknowledge test result should deliver event to state machine without remove flag`() {
        every { stateMachine.readState() } returns Default()
        every { testResultsProvider.testResults } returns mapOf(positiveTestResult.diagnosisKeySubmissionToken to positiveTestResult)

        testSubject.onCreate()

        testSubject.acknowledgeTestResult()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResult, false)) }
    }

    @Test
    fun `latest test result positive with expired isolation should return PositiveWontBeInIsolation`() {
        every { stateMachine.readState() } returns Default()
        val expiredPositiveTestResult = positiveTestResult.copy(
            testEndDate = symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC).minus(10, ChronoUnit.DAYS)
        )
        every { testResultsProvider.testResults } returns mapOf(positiveTestResult.diagnosisKeySubmissionToken to expiredPositiveTestResult)

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onCreate()

        verify {
            viewStateObserver.onChanged(
                ViewState(PositiveWontBeInIsolation(expiredPositiveTestResult.diagnosisKeySubmissionToken), 0)
            )
        }
    }

    companion object {
        val testEndDate = Instant.parse("2020-07-25T12:00:00Z")!!
        val symptomsOnsetDate = LocalDate.parse("2020-07-20")!!
    }
}
