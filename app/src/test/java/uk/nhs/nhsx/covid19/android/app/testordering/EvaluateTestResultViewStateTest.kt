package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.state.CreateIsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.CreateIsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.DoNotTransition
import uk.nhs.nhsx.covid19.android.app.state.TestResultIsolationHandler.TransitionDueToTestResult.Transition
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.FoundTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.NoTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PlodWillContinueWithCurrentState
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class EvaluateTestResultViewStateTest {

    private val getHighestPriorityTestResult = mockk<GetHighestPriorityTestResult>()
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val testResultIsolationHandler = mockk<TestResultIsolationHandler>()
    private val computeTestResultViewStateOnPositive = mockk<ComputeTestResultViewStateOnPositive>()
    private val computeTestResultViewStateOnNegative = mockk<ComputeTestResultViewStateOnNegative>()
    private val computeTestResultViewStateOnVoid = mockk<ComputeTestResultViewStateOnVoid>()
    private val createIsolationLogicalState = mockk<CreateIsolationLogicalState>()
    private val createIsolationState = mockk<CreateIsolationState>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val evaluateTestResultViewState = EvaluateTestResultViewState(
        getHighestPriorityTestResult,
        isolationStateMachine,
        testResultIsolationHandler,
        computeTestResultViewStateOnPositive,
        computeTestResultViewStateOnNegative,
        computeTestResultViewStateOnVoid,
        createIsolationLogicalState,
        createIsolationState,
        fixedClock
    )

    private val testResult = mockk<ReceivedTestResult>()
    private val stateTransition = DoNotTransition(preventKeySubmission = false, keySharingInfo = mockk())
    private val currentState = mockk<IsolationState>()
    private val newIsolationState = mockk<IsolationState>()
    private val currentInfo = mockk<IsolationInfo>()
    private val currentLogicalState = mockk<IsolationLogicalState>()
    private val newLogicalState = mockk<IsolationLogicalState>()
    private val now = Instant.now(fixedClock)

    private val isolationHelper = IsolationHelper(fixedClock)

    @Before
    fun setUp() {
        every { getHighestPriorityTestResult() } returns FoundTestResult(testResult)
        every { isolationStateMachine.readState() } returns currentState
        every { currentState.toIsolationInfo() } returns currentInfo
        every { createIsolationLogicalState(currentState) } returns currentLogicalState
        every { createIsolationState(currentInfo) } returns newIsolationState
        every { createIsolationLogicalState(newIsolationState) } returns newLogicalState

        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(currentState, testResult, now)
        } returns stateTransition
    }

    @Test
    fun `when no unacknowledged test result found then return view state Ignore`() {
        every { getHighestPriorityTestResult() } returns NoTestResult
        every { isolationStateMachine.remainingDaysInIsolation() } returns DEFAULT_REMAINING_DAYS_IN_ISOLATION.toLong()

        val result = evaluateTestResultViewState()

        assertEquals(expected = ViewState(Ignore, DEFAULT_REMAINING_DAYS_IN_ISOLATION), result)
    }

    @Test
    fun `when most relevant unacknowledged test result is positive and results in state transition`() {
        val expectedViewState = mockk<TestResultViewState>()
        val newIsolationState = isolationHelper.selfAssessment().asIsolation()
        val newIsolationInfo = newIsolationState.toIsolationInfo()
        val stateTransition = Transition(newIsolationInfo, keySharingInfo = mockk())

        val isolationLogicalState = mockk<IsolationLogicalState>()
        every { createIsolationState(newIsolationInfo) } returns newIsolationState
        every { createIsolationLogicalState(newIsolationState) } returns isolationLogicalState

        every { testResult.testResult } returns POSITIVE
        every {
            testResultIsolationHandler.computeTransitionWithTestResultAcknowledgment(currentState, testResult, now)
        } returns stateTransition
        every {
            computeTestResultViewStateOnPositive(currentLogicalState, isolationLogicalState, testResult)
        } returns expectedViewState
        every { isolationStateMachine.remainingDaysInIsolation(isolationLogicalState) } returns
            DEFAULT_REMAINING_DAYS_IN_ISOLATION.toLong()

        val result = evaluateTestResultViewState()

        assertEquals(expected = ViewState(expectedViewState, DEFAULT_REMAINING_DAYS_IN_ISOLATION), result)

        verify {
            computeTestResultViewStateOnPositive(currentLogicalState, isolationLogicalState, testResult)
            isolationStateMachine.remainingDaysInIsolation(isolationLogicalState)
        }
        confirmVerified(computeTestResultViewStateOnNegative, computeTestResultViewStateOnVoid)
    }

    @Test
    fun `when most relevant unacknowledged test result is positive but does not lead to state transition`() {
        val expectedViewState = mockk<TestResultViewState>()
        every { testResult.testResult } returns POSITIVE
        every {
            computeTestResultViewStateOnPositive(currentLogicalState, newLogicalState, testResult)
        } returns expectedViewState
        every { isolationStateMachine.remainingDaysInIsolation(newLogicalState) } returns
            DEFAULT_REMAINING_DAYS_IN_ISOLATION.toLong()

        val result = evaluateTestResultViewState()

        assertEquals(expected = ViewState(expectedViewState, DEFAULT_REMAINING_DAYS_IN_ISOLATION), result)

        verify {
            computeTestResultViewStateOnPositive(currentLogicalState, newLogicalState, testResult)
            isolationStateMachine.remainingDaysInIsolation(newLogicalState)
        }
        confirmVerified(computeTestResultViewStateOnNegative, computeTestResultViewStateOnVoid)
    }

    @Test
    fun `when most relevant unacknowledged test result is negative`() {
        val expectedViewState = mockk<TestResultViewState>()

        every { testResult.testResult } returns NEGATIVE
        every { computeTestResultViewStateOnNegative(currentLogicalState, newLogicalState) } returns expectedViewState
        every { isolationStateMachine.remainingDaysInIsolation(newLogicalState) } returns
            DEFAULT_REMAINING_DAYS_IN_ISOLATION.toLong()

        val result = evaluateTestResultViewState()

        assertEquals(expected = ViewState(expectedViewState, DEFAULT_REMAINING_DAYS_IN_ISOLATION), result)

        verify {
            computeTestResultViewStateOnNegative(currentLogicalState, newLogicalState)
            isolationStateMachine.remainingDaysInIsolation(newLogicalState)
        }
        confirmVerified(computeTestResultViewStateOnPositive, computeTestResultViewStateOnVoid)
    }

    @Test
    fun `when most relevant unacknowledged test result is void`() {
        val expectedViewState = mockk<TestResultViewState>()

        every { testResult.testResult } returns VOID
        every { computeTestResultViewStateOnVoid(currentLogicalState) } returns expectedViewState
        every { isolationStateMachine.remainingDaysInIsolation(newLogicalState) } returns
            DEFAULT_REMAINING_DAYS_IN_ISOLATION.toLong()

        val result = evaluateTestResultViewState()

        assertEquals(expected = ViewState(expectedViewState, DEFAULT_REMAINING_DAYS_IN_ISOLATION), result)

        verify {
            computeTestResultViewStateOnVoid(currentLogicalState)
            isolationStateMachine.remainingDaysInIsolation(newLogicalState)
        }
        confirmVerified(computeTestResultViewStateOnPositive, computeTestResultViewStateOnNegative)
    }

    @Test
    fun `when most relevant unacknowledged test result is plod`() {
        every { testResult.testResult } returns PLOD
        every { isolationStateMachine.remainingDaysInIsolation(newLogicalState) } returns
            DEFAULT_REMAINING_DAYS_IN_ISOLATION.toLong()

        val result = evaluateTestResultViewState()

        assertEquals(
            expected = ViewState(PlodWillContinueWithCurrentState, DEFAULT_REMAINING_DAYS_IN_ISOLATION),
            result
        )

        verify { isolationStateMachine.remainingDaysInIsolation(newLogicalState) }
        confirmVerified(
            computeTestResultViewStateOnPositive,
            computeTestResultViewStateOnNegative,
            computeTestResultViewStateOnVoid
        )
    }

    companion object {
        private const val DEFAULT_REMAINING_DAYS_IN_ISOLATION = 1
    }
}
