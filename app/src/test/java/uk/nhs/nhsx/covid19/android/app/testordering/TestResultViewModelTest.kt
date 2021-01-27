package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.SubmitEmptyData
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW_AFTER_POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.KEY_SUBMISSION
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
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

    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxed = true)
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val submitEmptyData = mockk<SubmitEmptyData>(relaxed = true)
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxed = true)
    private val fixedClock = Clock.fixed(symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)

    private val navigateToShareKeysObserver = mockk<Observer<ReceivedTestResult>>(relaxed = true)
    private val finishActivityObserver = mockk<Observer<Void>>(relaxed = true)

    private val testSubject =
        TestResultViewModel(
            unacknowledgedTestResultsProvider,
            relevantTestResultProvider,
            isolationConfigurationProvider,
            stateMachine,
            submitEmptyData,
            submitFakeExposureWindows,
            fixedClock
        )

    private val isolationState = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays()
    )

    private val isolationStateIndexCaseOnly = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(
            LocalDate.now(),
            expiryDate = symptomsOnsetDate.plus(7, ChronoUnit.DAYS),
            selfAssessment = false
        )
    )

    private val positiveTestResult = ReceivedTestResult(
        "token1",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true
    )
    private val negativeTestResult = ReceivedTestResult(
        "token3",
        testEndDate = testEndDate,
        testResult = NEGATIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true
    )
    private val voidTestResult = ReceivedTestResult(
        "token5",
        testEndDate = testEndDate,
        testResult = VOID,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true
    )

    @Test
    fun `empty unacknowledged test results should return Ignore`() =
        runBlocking {
            every { stateMachine.readState() } returns Default()
            every { unacknowledgedTestResultsProvider.testResults } returns emptyList()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(Ignore, 0)) }
        }

    // Case C
    @Test
    fun `relevant test result positive, unacknowledged positive, currently in isolation and will stay in isolation should return PositiveContinueIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns true
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
            every { stateMachine.readState() } returns isolationState

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveContinueIsolation, 0)
                )
            }
        }

    // Case G
    @Test
    fun `relevant test result positive, unacknowledged positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns true
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
            every { stateMachine.readState() } returns Default(previousIsolation = isolationStateIndexCaseOnly)

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    // Case E
    @Test
    fun `relevant test result not positive, unacknowledged negative, currently not in isolation and no previous isolation should return NegativeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns false
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeNotInIsolation, 0)) }
        }

    // Case ?
    @Test
    fun `relevant test result not positive, unacknowledged negative and currently in isolation should return NegativeWillBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns false
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns isolationState

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWillBeInIsolation, 0)) }
        }

    // Case A
    @Test
    fun `relevant test result not positive, unacknowledged negative and currently in isolation as index case only should return NegativeWontBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns false
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns isolationStateIndexCaseOnly

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWontBeInIsolation, 0)) }
        }

    // Case D
    @Test
    fun `relevant test result positive, unacknowledged negative and currently in isolation should return PositiveThenNegativeWillBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns true
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns isolationState

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(PositiveThenNegativeWillBeInIsolation, 0)) }
        }

    // Case F
    @Test
    fun `relevant test result not positive, unacknowledged void and currently not in isolation should return VoidNotInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns false
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidNotInIsolation, 0)) }
        }

    // Case B
    @Test
    fun `relevant test result not positive, unacknowledged void and currently in isolation should return VoidWillBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns false
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
            every { stateMachine.readState() } returns isolationState

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidWillBeInIsolation, 0)) }
        }

    // Case G
    @Test
    fun `relevant test result positive, unacknowledged void and positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns true
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResult
            )
            every { stateMachine.readState() } returns Default(previousIsolation = isolationStateIndexCaseOnly)

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    // Case H
    @Test
    fun `relevant test result positive, unacknowledged void and positive, currently not in isolation and no previous isolation should return PositiveWillBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns true
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResult
            )
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolation, 0)
                )
            }
        }

    // Case G
    @Test
    fun `relevant test result positive, unacknowledged negative and positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns true
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResult
            )
            every { stateMachine.readState() } returns Default(previousIsolation = isolationStateIndexCaseOnly)

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    // Case H
    @Test
    fun `relevant test result positive, unacknowledged negative and positive, currently not in isolation and no previous isolation return PositiveWillBeInIsolation`() =
        runBlocking {
            every { relevantTestResultProvider.isTestResultPositive() } returns true
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResult
            )
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolation, 0)
                )
            }
        }

    @Test
    fun `unacknowledged test result positive with expired isolation should return PositiveWontBeInIsolation`() =
        runBlocking {
            val expiredPositiveTestResult = positiveTestResult.copy(
                testEndDate = symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC).minus(10, ChronoUnit.DAYS)
            )
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(expiredPositiveTestResult)
            every { stateMachine.readState() } returns Default()

            testSubject.viewState().observeForever(viewStateObserver)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
            coVerify(exactly = 0) { submitEmptyData.invoke(KEY_SUBMISSION) }
            coVerify(exactly = 0) { submitFakeExposureWindows.invoke(any(), any()) }
        }

    @Test
    fun `acknowledge negative test result should deliver event to state machine`() {
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
        every { stateMachine.readState() } returns isolationState

        testSubject.onCreate()

        testSubject.acknowledgeTestResultIfNecessary()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(negativeTestResult)) }
        coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
        coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }
    }

    @Test
    fun `acknowledge void test result should deliver event to state machine`() {
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
        every { stateMachine.readState() } returns isolationState

        testSubject.onCreate()

        testSubject.acknowledgeTestResultIfNecessary()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(voidTestResult)) }
        coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
        coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }
    }

    @Test
    fun `acknowledge positive test result should do nothing`() {
        every { stateMachine.readState() } returns Default()
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)

        testSubject.onCreate()

        testSubject.acknowledgeTestResultIfNecessary()

        verify(exactly = 0) { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResult)) }
        coVerify(exactly = 0) { submitEmptyData.invoke(any()) }
        coVerify(exactly = 0) { submitFakeExposureWindows.invoke(any(), any()) }
    }

    @Test
    fun `clicking action button for positive test result when diagnosis key submission is supported triggers navigation event`() {
        testSubject.testResult = positiveTestResult.copy(diagnosisKeySubmissionSupported = true)
        testSubject.navigateToShareKeys().observeForever(navigateToShareKeysObserver)
        testSubject.finishActivity().observeForever(finishActivityObserver)

        testSubject.onActionButtonForPositiveTestResultClicked()

        verify { navigateToShareKeysObserver.onChanged(positiveTestResult) }
        verify(exactly = 0) { stateMachine.processEvent(OnTestResultAcknowledge(testSubject.testResult)) }
        coVerify(exactly = 0) { submitEmptyData.invoke(KEY_SUBMISSION) }
        coVerify(exactly = 0) { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }
        verify(exactly = 0) { finishActivityObserver.onChanged(any()) }
    }

    @Test
    fun `clicking action button for positive test result when diagnosis key submission is not supported acknowledges test result and finishes activity`() = runBlocking {
        testSubject.testResult = positiveTestResult.copy(diagnosisKeySubmissionSupported = false)
        testSubject.navigateToShareKeys().observeForever(navigateToShareKeysObserver)
        testSubject.finishActivity().observeForever(finishActivityObserver)

        testSubject.onActionButtonForPositiveTestResultClicked()

        verify(exactly = 0) { navigateToShareKeysObserver.onChanged(any()) }
        verify { stateMachine.processEvent(OnTestResultAcknowledge(testSubject.testResult)) }
        coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
        coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }
        verify { finishActivityObserver.onChanged(null) }
    }

    companion object {
        val testEndDate = Instant.parse("2020-07-25T12:00:00Z")!!
        val symptomsOnsetDate = LocalDate.parse("2020-07-20")!!
    }
}
