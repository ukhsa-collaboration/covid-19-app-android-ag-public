package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
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
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransitionButStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.TransitionAndStoreTestResult
import uk.nhs.nhsx.covid19.android.app.state.hasConfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.hasPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.hasUnconfirmedPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class TestResultViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxed = true)
    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val testResultIsolationHandler = mockk<TestResultIsolationHandler>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val submitEmptyData = mockk<SubmitEmptyData>(relaxed = true)
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxed = true)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)

    private val navigationObserver = mockk<Observer<NavigationEvent>>(relaxed = true)

    private val testSubject =
        TestResultViewModel(
            unacknowledgedTestResultsProvider,
            relevantTestResultProvider,
            testResultIsolationHandler,
            stateMachine,
            submitEmptyData,
            submitFakeExposureWindows
        )

    private val isolationState = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays()
    )

    private val isolationStateContactCaseOnly = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            startDate = contactDate,
            notificationDate = contactDate.plus(1, ChronoUnit.DAYS),
            expiryDate = LocalDateTime.ofInstant(contactDate.plus(7, ChronoUnit.DAYS), ZoneOffset.UTC).toLocalDate()
        )
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

    private val isolationStateIndexCaseSymptomaticOnly = Isolation(
        isolationStart = symptomsOnsetDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(
            LocalDate.now(),
            expiryDate = symptomsOnsetDate.plus(7, ChronoUnit.DAYS),
            selfAssessment = true
        )
    )

    private val defaultWithPreviousIsolationIndexCaseOnly =
        Default(previousIsolation = isolationStateIndexCaseOnly)

    private val positiveTestResult = ReceivedTestResult(
        "token1",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )
    private val positiveTestResultIndicative = ReceivedTestResult(
        "token1",
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = true
    )
    private val negativeTestResult = ReceivedTestResult(
        "token3",
        testEndDate = testEndDate,
        testResult = NEGATIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )
    private val voidTestResult = ReceivedTestResult(
        "token5",
        testEndDate = testEndDate,
        testResult = VOID,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )

    @Before
    fun setUp() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigationEvent().observeForever(navigationObserver)
    }

    @Test
    fun `empty unacknowledged test results should return Ignore`() =
        runBlocking {
            every { stateMachine.readState() } returns Default()
            every { unacknowledgedTestResultsProvider.testResults } returns emptyList()

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(Ignore, 0)) }
        }

    // Case C
    @Test
    fun `relevant test result confirmed positive, unacknowledged confirmed positive, currently in isolation and will stay in isolation should return PositiveContinueIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
            every { stateMachine.readState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    isolationState,
                    positiveTestResult
                )
            } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveContinueIsolation, 0)
                )
            }
        }

    @Test
    fun `relevant test result confirmed positive from before isolation, unacknowledged indicative positive, currently in isolation and will stay in isolation should return PositiveWillBeInIsolationAndOrderTest`() =
        runBlocking {
            setPreviousTestConfirmed(
                RelevantVirologyTestResult.POSITIVE,
                fromCurrentIsolation = false
            )
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
            every { stateMachine.readState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    isolationState,
                    positiveTestResultIndicative
                )
            } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolationAndOrderTest, 0)
                )
            }
        }

    @Test
    fun `relevant test result confirmed positive from current isolation, unacknowledged indicative positive, currently in isolation and will stay in isolation should return PositiveContinueIsolationNoChange`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
            every { stateMachine.readState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    isolationState,
                    positiveTestResultIndicative
                )
            } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveContinueIsolationNoChange, 0)
                )
            }
        }

    // Case G
    @Test
    fun `relevant test result confirmed positive, unacknowledged confirmed positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
            every { stateMachine.readState() } returns defaultWithPreviousIsolationIndexCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    defaultWithPreviousIsolationIndexCaseOnly,
                    positiveTestResult
                )
            } returns DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    // Case G
    @Test
    fun `relevant test result confirmed positive, unacknowledged indicative positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
            every { stateMachine.readState() } returns defaultWithPreviousIsolationIndexCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    defaultWithPreviousIsolationIndexCaseOnly,
                    positiveTestResultIndicative
                )
            } returns DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    // Case E
    @Test
    fun `no relevant test result, unacknowledged negative, currently not in isolation and no previous isolation should return NegativeNotInIsolation`() =
        runBlocking {
            val state = Default()
            setNoPreviousTest()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns state
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, negativeTestResult) } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeNotInIsolation, 0)) }
        }

    // Case ?
    @Test
    fun `no relevant test result, unacknowledged negative and currently in isolation should return NegativeWillBeInIsolation`() =
        runBlocking {
            setNoPreviousTest()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns isolationStateContactCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    isolationStateContactCaseOnly,
                    negativeTestResult
                )
            } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWillBeInIsolation, 0)) }
        }

    // Case A
    @Test
    fun `no relevant test result, unacknowledged negative and currently in isolation as index case only should return NegativeWontBeInIsolation`() =
        runBlocking {
            setNoPreviousTest()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns isolationStateIndexCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    isolationStateIndexCaseOnly,
                    negativeTestResult
                )
            } returns
                TransitionAndStoreTestResult(
                    newState = defaultWithPreviousIsolationIndexCaseOnly,
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWontBeInIsolation, 0)) }
        }

    // Case D
    @Test
    fun `relevant test result confirmed positive, unacknowledged negative and currently in isolation should return NegativeAfterPositiveOrSymptomaticWillBeInIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns isolationStateIndexCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    isolationStateIndexCaseOnly,
                    negativeTestResult
                )
            } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeAfterPositiveOrSymptomaticWillBeInIsolation, 0)) }
        }

    // Case D
    @Test
    fun `symptomatic, unacknowledged negative and currently in isolation should return NegativeAfterPositiveOrSymptomaticWillBeInIsolation`() =
        runBlocking {
            setNoPreviousTest()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns isolationStateIndexCaseSymptomaticOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    isolationStateIndexCaseSymptomaticOnly,
                    negativeTestResult
                )
            } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Overwrite)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeAfterPositiveOrSymptomaticWillBeInIsolation, 0)) }
        }

    // Case A
    @Test
    fun `relevant test result indicative positive, unacknowledged negative and currently in isolation should return NegativeWontBeInIsolation`() =
        runBlocking {
            setPreviousTestIndicativePositive()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    isolationState,
                    negativeTestResult
                )
            } returns
                TransitionAndStoreTestResult(
                    newState = Default(previousIsolation = isolationState),
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWontBeInIsolation, 0)) }
        }

    // Case F
    @Test
    fun `no relevant test result, unacknowledged void and currently not in isolation should return VoidNotInIsolation`() =
        runBlocking {
            val state = Default()
            setNoPreviousTest()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
            every { stateMachine.readState() } returns state
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, voidTestResult) } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidNotInIsolation, 0)) }
        }

    // Case B
    @Test
    fun `no relevant test result, unacknowledged void and currently in isolation should return VoidWillBeInIsolation`() =
        runBlocking {
            setNoPreviousTest()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
            every { stateMachine.readState() } returns isolationState
            every { testResultIsolationHandler.computeTransitionWithTestResult(isolationState, voidTestResult) } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidWillBeInIsolation, 0)) }
        }

    // Case B
    @Test
    fun `relevant test result confirmed negative, unacknowledged void and currently in isolation should return VoidWillBeInIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.NEGATIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
            every { stateMachine.readState() } returns isolationState
            every { testResultIsolationHandler.computeTransitionWithTestResult(isolationState, voidTestResult) } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidWillBeInIsolation, 0)) }
        }

    // Case G
    @Test
    fun `relevant test result confirmed positive, unacknowledged void and confirmed positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResult
            )
            every { stateMachine.readState() } returns defaultWithPreviousIsolationIndexCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    defaultWithPreviousIsolationIndexCaseOnly,
                    positiveTestResult
                )
            } returns DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    // Case G
    @Test
    fun `relevant test result confirmed positive, unacknowledged void and indicative positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResultIndicative
            )
            every { stateMachine.readState() } returns defaultWithPreviousIsolationIndexCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    defaultWithPreviousIsolationIndexCaseOnly,
                    positiveTestResultIndicative
                )
            } returns DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    @Test
    fun `relevant test result confirmed positive, unacknowledged void and confirmed positive, currently not in isolation and no previous isolation should return PositiveWillBeInIsolation`() =
        runBlocking {
            val state = Default()
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResult
            )
            every { stateMachine.readState() } returns state
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, positiveTestResult) } returns
                TransitionAndStoreTestResult(
                    newState = isolationStateIndexCaseOnly,
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolation, 0)
                )
            }
        }

    @Test
    fun `relevant test result confirmed positive, unacknowledged void and indicative positive, currently not in isolation and no previous isolation should return PositiveWillBeInIsolation`() =
        runBlocking {
            val state = Default()
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResultIndicative
            )
            every { stateMachine.readState() } returns state
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    state,
                    positiveTestResultIndicative
                )
            } returns
                TransitionAndStoreTestResult(
                    newState = isolationStateIndexCaseOnly,
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolationAndOrderTest, 0)
                )
            }
        }

    // Case G
    @Test
    fun `relevant test result confirmed positive, unacknowledged negative and confirmed positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResult
            )
            every { stateMachine.readState() } returns defaultWithPreviousIsolationIndexCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    defaultWithPreviousIsolationIndexCaseOnly,
                    positiveTestResult
                )
            } returns DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    // Case G
    @Test
    fun `relevant test result confirmed positive, unacknowledged negative and indicative positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResultIndicative
            )
            every { stateMachine.readState() } returns defaultWithPreviousIsolationIndexCaseOnly
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    defaultWithPreviousIsolationIndexCaseOnly,
                    positiveTestResultIndicative
                )
            } returns DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    // Case H
    @Test
    fun `relevant test result confirmed positive, unacknowledged negative and confirmed positive, currently not in isolation and no previous isolation return PositiveWillBeInIsolation`() =
        runBlocking {
            val state = Default()
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResult
            )
            every { stateMachine.readState() } returns state
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, positiveTestResult) } returns
                TransitionAndStoreTestResult(
                    newState = isolationStateIndexCaseOnly,
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolation, 0)
                )
            }
        }

    @Test
    fun `relevant test result confirmed positive, unacknowledged negative and indicative positive, currently not in isolation and no previous isolation return PositiveWillBeInIsolation`() =
        runBlocking {
            val state = Default()
            setPreviousTestConfirmed(RelevantVirologyTestResult.POSITIVE)
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResultIndicative
            )
            every { stateMachine.readState() } returns state
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    state,
                    positiveTestResultIndicative
                )
            } returns
                TransitionAndStoreTestResult(
                    newState = isolationStateIndexCaseOnly,
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWillBeInIsolationAndOrderTest, 0)
                )
            }
        }

    @Test
    fun `unacknowledged test result confirmed positive with expired isolation should return PositiveWontBeInIsolation`() =
        runBlocking {
            val state = Default()
            val expiredPositiveTestResult = positiveTestResult.copy(
                testEndDate = symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC).minus(10, ChronoUnit.DAYS)
            )
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(expiredPositiveTestResult)
            every { stateMachine.readState() } returns state
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    state,
                    expiredPositiveTestResult
                )
            } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

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
    fun `unacknowledged test result indicative positive with expired isolation should return PositiveWontBeInIsolation`() =
        runBlocking {
            val state = Default()
            val expiredPositiveTestResult = positiveTestResultIndicative.copy(
                testEndDate = symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC).minus(10, ChronoUnit.DAYS)
            )
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(expiredPositiveTestResult)
            every { stateMachine.readState() } returns state
            every {
                testResultIsolationHandler.computeTransitionWithTestResult(
                    state,
                    expiredPositiveTestResult
                )
            } returns
                DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

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
    fun `button click for negative test result should acknowledge test result and finish activity`() {
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
        every { stateMachine.readState() } returns isolationState
        every { testResultIsolationHandler.computeTransitionWithTestResult(isolationState, negativeTestResult) } returns
            DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

        testSubject.onCreate()

        testSubject.onActionButtonClicked()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(negativeTestResult)) }
        coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
        coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

        verify { navigationObserver.onChanged(NavigationEvent.Finish) }
    }

    @Test
    fun `back press for negative test result should acknowledge test result`() {
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
        every { stateMachine.readState() } returns isolationState
        every { testResultIsolationHandler.computeTransitionWithTestResult(isolationState, negativeTestResult) } returns
            DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

        testSubject.onCreate()

        testSubject.onBackPressed()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(negativeTestResult)) }
        coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
        coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

        verify(exactly = 0) { navigationObserver.onChanged(any()) }
    }

    @Test
    fun `button click for void test result should acknowledge test result and navigate to order test`() {
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
        every { stateMachine.readState() } returns isolationState
        every { testResultIsolationHandler.computeTransitionWithTestResult(isolationState, voidTestResult) } returns
            DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

        testSubject.onCreate()

        testSubject.onActionButtonClicked()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(voidTestResult)) }
        coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
        coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

        verify { navigationObserver.onChanged(NavigationEvent.NavigateToOrderTest) }
    }

    @Test
    fun `back press for void test result should acknowledge test result`() {
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
        every { stateMachine.readState() } returns isolationState
        every { testResultIsolationHandler.computeTransitionWithTestResult(isolationState, voidTestResult) } returns
            DoNotTransitionButStoreTestResult(TestResultStorageOperation.Ignore)

        testSubject.onCreate()

        testSubject.onBackPressed()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(voidTestResult)) }
        coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
        coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

        verify(exactly = 0) { navigationObserver.onChanged(any()) }
    }

    @Test
    fun `button click for confirmed positive test result should do nothing and navigate to share keys`() {
        val state = Default()
        every { stateMachine.readState() } returns state
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
        every { testResultIsolationHandler.computeTransitionWithTestResult(state, positiveTestResult) } returns
            TransitionAndStoreTestResult(
                newState = isolationStateIndexCaseOnly,
                testResultStorageOperation = TestResultStorageOperation.Ignore
            )

        testSubject.onCreate()

        testSubject.onActionButtonClicked()

        verify(exactly = 0) { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResult)) }
        coVerify(exactly = 0) { submitEmptyData.invoke(any()) }
        coVerify(exactly = 0) { submitFakeExposureWindows.invoke(any(), any()) }

        verify { navigationObserver.onChanged(NavigationEvent.NavigateToShareKeys(positiveTestResult)) }
    }

    @Test
    fun `back press for confirmed positive test result should do nothing`() {
        val state = Default()
        every { stateMachine.readState() } returns state
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
        every { testResultIsolationHandler.computeTransitionWithTestResult(state, positiveTestResult) } returns
            TransitionAndStoreTestResult(
                newState = isolationStateIndexCaseOnly,
                testResultStorageOperation = TestResultStorageOperation.Ignore
            )

        testSubject.onCreate()

        testSubject.onBackPressed()

        verify(exactly = 0) { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResult)) }
        coVerify(exactly = 0) { submitEmptyData.invoke(any()) }
        coVerify(exactly = 0) { submitFakeExposureWindows.invoke(any(), any()) }

        verify(exactly = 0) { navigationObserver.onChanged(any()) }
    }

    @Test
    fun `button click for indicative positive test result should acknowledge test result and navigate to order test`() {
        val state = Default()
        every { stateMachine.readState() } returns state
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
        every {
            testResultIsolationHandler.computeTransitionWithTestResult(
                state,
                positiveTestResultIndicative
            )
        } returns
            TransitionAndStoreTestResult(
                newState = isolationStateIndexCaseOnly,
                testResultStorageOperation = TestResultStorageOperation.Ignore
            )

        testSubject.onCreate()

        testSubject.onActionButtonClicked()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResultIndicative)) }
        coVerify { submitEmptyData.invoke(any()) }
        coVerify { submitFakeExposureWindows.invoke(any(), any()) }

        verify { navigationObserver.onChanged(NavigationEvent.NavigateToOrderTest) }
    }

    @Test
    fun `back press for indicative positive test result should acknowledge test result`() {
        val state = Default()
        every { stateMachine.readState() } returns state
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
        every {
            testResultIsolationHandler.computeTransitionWithTestResult(
                state,
                positiveTestResultIndicative
            )
        } returns
            TransitionAndStoreTestResult(
                newState = isolationStateIndexCaseOnly,
                testResultStorageOperation = TestResultStorageOperation.Ignore
            )

        testSubject.onCreate()

        testSubject.onBackPressed()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResultIndicative)) }
        coVerify { submitEmptyData.invoke(any()) }
        coVerify { submitFakeExposureWindows.invoke(any(), any()) }

        verify(exactly = 0) { navigationObserver.onChanged(any()) }
    }

    @Test
    fun `button click for confirmed positive test result when diagnosis key submission is not supported acknowledges test result and finishes activity`() =
        runBlocking {
            val state = Default()
            val testResult = positiveTestResult.copy(diagnosisKeySubmissionSupported = false)
            every { stateMachine.readState() } returns state
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, testResult) } returns
                TransitionAndStoreTestResult(
                    newState = isolationStateIndexCaseOnly,
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            testSubject.onActionButtonClicked()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
            coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

            verify { navigationObserver.onChanged(NavigationEvent.Finish) }
        }

    @Test
    fun `button click for confirmed positive test result when diagnosis key submission is supported but prevented acknowledges test result and finishes activity`() =
        runBlocking {
            val state = Default()
            val testResult = positiveTestResult
            every { stateMachine.readState() } returns state
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, testResult) } returns
                TransitionDueToTestResult.Ignore(preventKeySubmission = true)

            testSubject.onCreate()

            testSubject.onActionButtonClicked()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
            coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

            verify { navigationObserver.onChanged(NavigationEvent.Finish) }
        }

    @Test
    fun `back press for confirmed positive test result when diagnosis key submission is not supported acknowledges test result`() =
        runBlocking {
            val state = Default()
            val testResult = positiveTestResult.copy(diagnosisKeySubmissionSupported = false)
            every { stateMachine.readState() } returns state
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, testResult) } returns
                TransitionAndStoreTestResult(
                    newState = isolationStateIndexCaseOnly,
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            testSubject.onBackPressed()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
            coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

            verify(exactly = 0) { navigationObserver.onChanged(any()) }
        }

    @Test
    fun `back press for confirmed positive test result when diagnosis key submission is supported but prevented acknowledges test result`() =
        runBlocking {
            val state = Default()
            val testResult = positiveTestResult
            every { stateMachine.readState() } returns state
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, testResult) } returns
                TransitionDueToTestResult.Ignore(preventKeySubmission = true)

            testSubject.onCreate()

            testSubject.onBackPressed()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
            coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

            verify(exactly = 0) { navigationObserver.onChanged(any()) }
        }

    @Test
    fun `back press for indicative positive test result when diagnosis key submission is not supported acknowledges test result`() =
        runBlocking {
            val state = Default()
            val testResult = positiveTestResultIndicative.copy(diagnosisKeySubmissionSupported = false)
            every { stateMachine.readState() } returns state
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, testResult) } returns
                TransitionAndStoreTestResult(
                    newState = isolationStateIndexCaseOnly,
                    testResultStorageOperation = TestResultStorageOperation.Ignore
                )

            testSubject.onCreate()

            testSubject.onBackPressed()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
            coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

            verify(exactly = 0) { navigationObserver.onChanged(any()) }
        }

    @Test
    fun `back press for indicative positive test result when diagnosis key submission is supported but prevented acknowledges test result`() =
        runBlocking {
            val state = Default()
            val testResult = positiveTestResultIndicative.copy(diagnosisKeySubmissionSupported = false)
            every { stateMachine.readState() } returns state
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every { testResultIsolationHandler.computeTransitionWithTestResult(state, testResult) } returns
                TransitionDueToTestResult.Ignore(preventKeySubmission = true)

            testSubject.onCreate()

            testSubject.onBackPressed()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            coVerify { submitEmptyData.invoke(KEY_SUBMISSION) }
            coVerify { submitFakeExposureWindows.invoke(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }

            verify(exactly = 0) { navigationObserver.onChanged(any()) }
        }

    private fun setPreviousTestIndicativePositive(fromCurrentIsolation: Boolean = true) {
        mockkStatic("uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachineKt")
        every { any<Isolation>().hasConfirmedPositiveTestResult(any()) } returns false
        every { any<Isolation>().hasUnconfirmedPositiveTestResult(any()) } returns fromCurrentIsolation
        every { any<Isolation>().hasPositiveTestResult(any()) } returns fromCurrentIsolation
    }

    private fun setPreviousTestConfirmed(
        testResult: RelevantVirologyTestResult,
        fromCurrentIsolation: Boolean = true
    ) {
        val isPositiveFromCurrentIsolation = testResult == RelevantVirologyTestResult.POSITIVE && fromCurrentIsolation

        mockkStatic("uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachineKt")
        every { any<Isolation>().hasConfirmedPositiveTestResult(any()) } returns isPositiveFromCurrentIsolation
        every { any<Isolation>().hasUnconfirmedPositiveTestResult(any()) } returns false
        every { any<Isolation>().hasPositiveTestResult(any()) } returns isPositiveFromCurrentIsolation
    }

    private fun setNoPreviousTest() {
        mockkStatic("uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachineKt")
        every { any<Isolation>().hasConfirmedPositiveTestResult(any()) } returns false
        every { any<Isolation>().hasUnconfirmedPositiveTestResult(any()) } returns false
        every { any<Isolation>().hasPositiveTestResult(any()) } returns false
    }

    companion object {
        val testEndDate = Instant.parse("2020-07-25T12:00:00Z")!!
        val symptomsOnsetDate = LocalDate.parse("2020-07-20")!!
        val contactDate = Instant.parse("2020-07-19T01:00:00Z")!!
    }
}
