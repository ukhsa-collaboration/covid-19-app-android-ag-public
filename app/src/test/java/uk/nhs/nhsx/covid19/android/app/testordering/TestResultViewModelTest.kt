package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmitObfuscationData
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.OnTestResultAcknowledge
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.state.asLogical
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class TestResultViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxed = true)
    private val testResultIsolationHandler = mockk<TestResultIsolationHandler>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val submitObfuscationData = mockk<SubmitObfuscationData>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val navigationObserver = mockk<Observer<NavigationEvent>>(relaxed = true)

    private val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject =
        TestResultViewModel(
            unacknowledgedTestResultsProvider,
            testResultIsolationHandler,
            stateMachine,
            submitObfuscationData,
            fixedClock
        )

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
        every { stateMachine.remainingDaysInIsolation(any()) } returns 0
        every { stateMachine.processEvent(any()) } returns mockk()
    }

    @Test
    fun `empty unacknowledged test results should return Ignore`() =
        runBlocking {
            every { stateMachine.readLogicalState() } returns isolationHelper.neverInIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns emptyList()

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(Ignore, 0)) }
        }

    // Case C
    @Test
    fun `relevant test result confirmed positive, unacknowledged confirmed positive, currently in isolation and will stay in isolation should return PositiveContinueIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(),
                indexInfo = isolationHelper.positiveTest(
                    acknowledgedTestResult(
                        result = RelevantVirologyTestResult.POSITIVE,
                        isConfirmed = true,
                        fromCurrentIsolation = false
                    )
                )
            ).asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResultIndicative,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(result = RelevantVirologyTestResult.POSITIVE, isConfirmed = true)
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResultIndicative,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResultIndicative,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    negativeTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeNotInIsolation, 0)) }
        }

    // Case ?
    @Test
    fun `no relevant test result, unacknowledged negative and currently in isolation should return NegativeWillBeInIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.contactCase().asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    negativeTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWillBeInIsolation, 0)) }
        }

    // Case A
    @Test
    fun `no relevant test result, unacknowledged negative and currently in isolation as index case only should return NegativeWontBeInIsolation`() =
        runBlocking {
            val isolation = isolationHelper.selfAssessment().asIsolation()
            val isolationState = isolation.asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    negativeTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns Transition(newState = isolation.expireIndexCase(), keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWontBeInIsolation, 0)) }
        }

    // Case D
    @Test
    fun `relevant test result confirmed positive, unacknowledged negative and currently in isolation should return NegativeAfterPositiveOrSymptomaticWillBeInIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    negativeTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeAfterPositiveOrSymptomaticWillBeInIsolation, 0)) }
        }

    // Case D
    @Test
    fun `symptomatic, unacknowledged negative and currently in isolation should return NegativeAfterPositiveOrSymptomaticWillBeInIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.selfAssessment().asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    negativeTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeAfterPositiveOrSymptomaticWillBeInIsolation, 0)) }
        }

    // Case A
    @Test
    fun `relevant test result indicative positive, unacknowledged negative and currently in isolation should return NegativeWontBeInIsolation`() =
        runBlocking {
            val isolation = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = false
                )
            ).asIsolation()
            val isolationState = isolation.asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    negativeTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns Transition(newState = isolation.expireIndexCase(), keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(NegativeWontBeInIsolation, 0)) }
        }

    // Case F
    @Test
    fun `no relevant test result, unacknowledged void and currently not in isolation should return VoidNotInIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    voidTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidNotInIsolation, 0)) }
        }

    // Case B
    @Test
    fun `no relevant test result, unacknowledged void and currently in isolation should return VoidWillBeInIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.contactCase().asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    voidTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidWillBeInIsolation, 0)) }
        }

    // Case B
    @Test
    fun `relevant test result confirmed negative, unacknowledged void and currently in isolation should return VoidWillBeInIsolation`() =
        runBlocking {
            val isolationState = IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(),
                indexInfo = isolationHelper.negativeTest(
                    acknowledgedTestResult(
                        result = RelevantVirologyTestResult.NEGATIVE,
                        isConfirmed = true
                    )
                )
            ).asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    voidTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify { viewStateObserver.onChanged(ViewState(VoidWillBeInIsolation, 0)) }
        }

    // Case G
    @Test
    fun `relevant test result confirmed positive, unacknowledged void and confirmed positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResult
            )
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResultIndicative
            )
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResultIndicative,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResult
            )
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                Transition(
                    newState = isolationHelper.positiveTest(positiveTestResult.toAcknowledgedTestResult()).asIsolation(),
                    keySharingInfo = null
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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                voidTestResult,
                positiveTestResultIndicative
            )
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResultIndicative,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                Transition(
                    newState = isolationHelper.positiveTest(positiveTestResultIndicative.toAcknowledgedTestResult()).asIsolation(),
                    keySharingInfo = null
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
    fun `no relevant test result unacknowledged negative and confirmed positive, currently not in isolation and previous isolation is index case should return PositiveWontBeInIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResult
            )
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResultIndicative
            )
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResultIndicative,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResult
            )
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                Transition(
                    newState = isolationHelper.positiveTest(positiveTestResult.toAcknowledgedTestResult()).asIsolation(),
                    keySharingInfo = null
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
            val isolationState = isolationHelper.positiveTest(
                acknowledgedTestResult(
                    result = RelevantVirologyTestResult.POSITIVE,
                    isConfirmed = true,
                    fromCurrentIsolation = false
                )
            ).asIsolation().asLogical()
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(
                negativeTestResult,
                positiveTestResultIndicative
            )
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    positiveTestResultIndicative,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                Transition(
                    newState = isolationHelper.positiveTest(positiveTestResultIndicative.toAcknowledgedTestResult())
                        .asIsolation(),
                    keySharingInfo = null
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
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            val expiredPositiveTestResult = positiveTestResult.copy(
                testEndDate = symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC).minus(10, ChronoUnit.DAYS)
            )
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(expiredPositiveTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    expiredPositiveTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    @Test
    fun `unacknowledged test result indicative positive with expired isolation should return PositiveWontBeInIsolation`() =
        runBlocking {
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            val expiredPositiveTestResult = positiveTestResultIndicative.copy(
                testEndDate = symptomsOnsetDate.atStartOfDay().toInstant(ZoneOffset.UTC).minus(10, ChronoUnit.DAYS)
            )
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(expiredPositiveTestResult)
            every { stateMachine.readLogicalState() } returns isolationState
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    expiredPositiveTestResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

            testSubject.onCreate()

            verify {
                viewStateObserver.onChanged(
                    ViewState(PositiveWontBeInIsolation, 0)
                )
            }
        }

    @Test
    fun `button click for negative test result should acknowledge test result and finish activity`() {
        val isolationState = isolationHelper.contactCase().asIsolation().asLogical()
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
        every { stateMachine.readLogicalState() } returns isolationState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                isolationState,
                negativeTestResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns
            DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

        testSubject.onCreate()

        testSubject.onActionButtonClicked()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(negativeTestResult)) }
        verify { submitObfuscationData() }
        verify { navigationObserver.onChanged(NavigationEvent.Finish) }
    }

    @Test
    fun `back press for negative test result should acknowledge test result`() {
        val isolationState = isolationHelper.contactCase().asIsolation().asLogical()
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(negativeTestResult)
        every { stateMachine.readLogicalState() } returns isolationState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                isolationState,
                negativeTestResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns
            DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

        testSubject.onCreate()

        testSubject.onBackPressed()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(negativeTestResult)) }
        verify { submitObfuscationData() }
        verify(exactly = 0) { navigationObserver.onChanged(any()) }
    }

    @Test
    fun `button click for void test result should acknowledge test result and navigate to order test`() {
        val isolationState = isolationHelper.contactCase().asIsolation().asLogical()
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
        every { stateMachine.readLogicalState() } returns isolationState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                isolationState,
                voidTestResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns
            DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

        testSubject.onCreate()

        testSubject.onActionButtonClicked()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(voidTestResult)) }
        verify { submitObfuscationData() }
        verify { navigationObserver.onChanged(NavigationEvent.NavigateToOrderTest) }
    }

    @Test
    fun `back press for void test result should acknowledge test result`() {
        val isolationState = isolationHelper.contactCase().asIsolation().asLogical()
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(voidTestResult)
        every { stateMachine.readLogicalState() } returns isolationState
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                isolationState,
                voidTestResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns
            DoNotTransition(preventKeySubmission = false, keySharingInfo = null)

        testSubject.onCreate()

        testSubject.onBackPressed()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(voidTestResult)) }
        verify { submitObfuscationData() }
        verify(exactly = 0) { navigationObserver.onChanged(any()) }
    }

    @Test
    fun `button click for confirmed positive test result should acknowledge test result and navigate to share keys`() {
        val isolationState = isolationHelper.neverInIsolation().asLogical()
        every { stateMachine.readLogicalState() } returns isolationState
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                isolationState,
                positiveTestResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns
            Transition(
                newState = isolationHelper.positiveTest(positiveTestResult.toAcknowledgedTestResult()).asIsolation(),
                keySharingInfo = null
            )

        testSubject.onCreate()

        testSubject.onActionButtonClicked()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResult)) }
        verify(exactly = 0) { submitObfuscationData() }

        verify { navigationObserver.onChanged(NavigationEvent.NavigateToShareKeys) }
    }

    @Test
    fun `back press for confirmed positive test result should acknowledge test result`() {
        val isolationState = isolationHelper.neverInIsolation().asLogical()
        every { stateMachine.readLogicalState() } returns isolationState
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResult)
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                isolationState,
                positiveTestResult,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns
            Transition(
                newState = isolationHelper.positiveTest(positiveTestResult.toAcknowledgedTestResult()).asIsolation(),
                keySharingInfo = null
            )

        testSubject.onCreate()

        testSubject.onBackPressed()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResult)) }
        verify { submitObfuscationData() }
        verify(exactly = 0) { navigationObserver.onChanged(any()) }
    }

    @Test
    fun `button click for indicative positive test result should acknowledge test result and navigate to order test`() {
        val isolationState = isolationHelper.neverInIsolation().asLogical()
        every { stateMachine.readLogicalState() } returns isolationState
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                isolationState,
                positiveTestResultIndicative,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns Transition(
            newState = isolationHelper.positiveTest(positiveTestResultIndicative.toAcknowledgedTestResult())
                .asIsolation(),
            keySharingInfo = null
        )

        testSubject.onCreate()

        testSubject.onActionButtonClicked()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResultIndicative)) }
        verify { submitObfuscationData() }
        verify { navigationObserver.onChanged(NavigationEvent.NavigateToOrderTest) }
    }

    @Test
    fun `back press for indicative positive test result should acknowledge test result`() {
        val isolationState = isolationHelper.neverInIsolation().asLogical()
        every { stateMachine.readLogicalState() } returns isolationState
        every { unacknowledgedTestResultsProvider.testResults } returns listOf(positiveTestResultIndicative)
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                isolationState,
                positiveTestResultIndicative,
                testAcknowledgedDate = Instant.now(fixedClock)
            )
        } returns Transition(
            newState = isolationHelper.positiveTest(positiveTestResultIndicative.toAcknowledgedTestResult()).asIsolation(),
            keySharingInfo = null
        )

        testSubject.onCreate()

        testSubject.onBackPressed()

        verify { stateMachine.processEvent(OnTestResultAcknowledge(positiveTestResultIndicative)) }
        verify { submitObfuscationData() }
        verify(exactly = 0) { navigationObserver.onChanged(any()) }
    }

    @Test
    fun `button click for confirmed positive test result when diagnosis key submission is not supported acknowledges test result and finishes activity`() =
        runBlocking {
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            val testResult = positiveTestResult.copy(diagnosisKeySubmissionSupported = false)
            every { stateMachine.readLogicalState() } returns isolationState
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    testResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                Transition(
                    newState = isolationHelper.positiveTest(testResult.toAcknowledgedTestResult()).asIsolation(),
                    keySharingInfo = null
                )

            testSubject.onCreate()

            testSubject.onActionButtonClicked()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            verify { submitObfuscationData() }

            verify { navigationObserver.onChanged(NavigationEvent.Finish) }
        }

    @Test
    fun `button click for confirmed positive test result when diagnosis key submission is supported but prevented acknowledges test result and finishes activity`() =
        runBlocking {
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            val testResult = positiveTestResult
            every { stateMachine.readLogicalState() } returns isolationState
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    testResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                DoNotTransition(preventKeySubmission = true, keySharingInfo = null)

            testSubject.onCreate()

            testSubject.onActionButtonClicked()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            verify { submitObfuscationData() }

            verify { navigationObserver.onChanged(NavigationEvent.Finish) }
        }

    @Test
    fun `back press for confirmed positive test result when diagnosis key submission is not supported acknowledges test result`() =
        runBlocking {
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            val testResult = positiveTestResult.copy(diagnosisKeySubmissionSupported = false)
            every { stateMachine.readLogicalState() } returns isolationState
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    testResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                Transition(
                    newState = isolationHelper.positiveTest(testResult.toAcknowledgedTestResult()).asIsolation(),
                    keySharingInfo = null
                )

            testSubject.onCreate()

            testSubject.onBackPressed()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            verify { submitObfuscationData() }
            verify(exactly = 0) { navigationObserver.onChanged(any()) }
        }

    @Test
    fun `back press for confirmed positive test result when diagnosis key submission is supported but prevented acknowledges test result`() =
        runBlocking {
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            val testResult = positiveTestResult
            every { stateMachine.readLogicalState() } returns isolationState
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    testResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                DoNotTransition(preventKeySubmission = true, keySharingInfo = null)

            testSubject.onCreate()

            testSubject.onBackPressed()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            verify { submitObfuscationData() }
            verify(exactly = 0) { navigationObserver.onChanged(any()) }
        }

    @Test
    fun `back press for indicative positive test result when diagnosis key submission is not supported acknowledges test result`() =
        runBlocking {
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            val testResult = positiveTestResultIndicative.copy(diagnosisKeySubmissionSupported = false)
            every { stateMachine.readLogicalState() } returns isolationState
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    testResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                Transition(
                    newState = isolationHelper.positiveTest(positiveTestResultIndicative.toAcknowledgedTestResult()).asIsolation(),
                    keySharingInfo = null
                )

            testSubject.onCreate()

            testSubject.onBackPressed()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            verify { submitObfuscationData() }
            verify(exactly = 0) { navigationObserver.onChanged(any()) }
        }

    @Test
    fun `back press for indicative positive test result when diagnosis key submission is supported but prevented acknowledges test result`() =
        runBlocking {
            val isolationState = isolationHelper.neverInIsolation().asLogical()
            val testResult = positiveTestResultIndicative.copy(diagnosisKeySubmissionSupported = false)
            every { stateMachine.readLogicalState() } returns isolationState
            every { unacknowledgedTestResultsProvider.testResults } returns listOf(testResult)
            every {
                testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(
                    isolationState,
                    testResult,
                    testAcknowledgedDate = Instant.now(fixedClock)
                )
            } returns
                DoNotTransition(preventKeySubmission = true, keySharingInfo = null)

            testSubject.onCreate()

            testSubject.onBackPressed()

            verify { stateMachine.processEvent(OnTestResultAcknowledge(testResult)) }
            verify { submitObfuscationData() }
            verify(exactly = 0) { navigationObserver.onChanged(any()) }
        }

    private fun acknowledgedTestResult(
        result: RelevantVirologyTestResult,
        isConfirmed: Boolean,
        fromCurrentIsolation: Boolean = true
    ): AcknowledgedTestResult {
        val testEndDate =
            if (fromCurrentIsolation) LocalDate.now(fixedClock)
            else LocalDate.now(fixedClock).minusDays(12)

        return AcknowledgedTestResult(
            testEndDate = testEndDate,
            testResult = result,
            acknowledgedDate = testEndDate,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = !isConfirmed,
            confirmedDate = null
        )
    }

    private fun IsolationState.expireIndexCase(): IsolationState =
        copy(
            indexInfo = (indexInfo as IndexCase)
                .copy(expiryDate = LocalDate.now(fixedClock))
        )

    private fun ReceivedTestResult.toAcknowledgedTestResult(): AcknowledgedTestResult {
        val result = testResult.toRelevantVirologyTestResult()
        if (result == null) {
            throw IllegalArgumentException("This function cannot be called with a $result test result")
        }
        return AcknowledgedTestResult(
            testEndDay(fixedClock),
            result,
            testKitType,
            acknowledgedDate = LocalDate.now(fixedClock),
            requiresConfirmatoryTest,
            confirmedDate = null
        )
    }

    companion object {
        val testEndDate = Instant.parse("2020-07-25T12:00:00Z")!!
        val symptomsOnsetDate = LocalDate.parse("2020-07-20")!!
    }
}
