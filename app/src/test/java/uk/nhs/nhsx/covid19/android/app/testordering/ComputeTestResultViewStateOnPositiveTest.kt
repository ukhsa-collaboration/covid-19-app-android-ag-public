package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.ButtonAction.ShareKeys
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ComputeTestResultViewStateOnPositiveTest {

    private val isKeySubmissionSupported = mockk<IsKeySubmissionSupported>()
    private val evaluateTestResultButtonAction = mockk<EvaluateTestResultButtonAction>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val computeTestResultViewStateOnPositive = ComputeTestResultViewStateOnPositive(
        isKeySubmissionSupported,
        evaluateTestResultButtonAction,
        fixedClock
    )

    private val testResult = mockk<ReceivedTestResult>()
    private val buttonAction = ShareKeys(bookFollowUpTest = true)

    @Before
    fun setUp() {
        every { testResult.requiresConfirmatoryTest } returns true
        every { isKeySubmissionSupported(testResult) } returns false
    }

    @Test
    fun `when new state is not active isolation then return PositiveWontBeInIsolation with expected button action`() {
        val currentState = mockk<PossiblyIsolating>()
        val newState = mockk<PossiblyIsolating>()

        every { evaluateTestResultButtonAction(currentState, newState, testResult) } returns buttonAction
        every { newState.isActiveIsolation(fixedClock) } returns false

        val result = computeTestResultViewStateOnPositive(currentState, newState, testResult)

        assertEquals(PositiveWontBeInIsolation(buttonAction), result)
    }

    @Test
    fun `when current state is active isolation and new state is also active isolation with test that does not require confirmation return PositiveContinueIsolation`() {
        val currentState = mockk<IsolationLogicalState>()
        val newState = mockk<IsolationLogicalState>()

        every { testResult.requiresConfirmatoryTest } returns false
        every { evaluateTestResultButtonAction(currentState, newState, testResult) } returns buttonAction
        every { currentState.isActiveIsolation(fixedClock) } returns true
        every { newState.isActiveIsolation(fixedClock) } returns true

        val result = computeTestResultViewStateOnPositive(currentState, newState, testResult)

        assertEquals(PositiveContinueIsolation(buttonAction), result)
    }

    @Test
    fun `when current state is not active isolation and new state is active isolation with test that does not require confirmation then return PositiveWillBeInIsolation`() {
        val currentState = mockk<IsolationLogicalState>()
        val newState = mockk<IsolationLogicalState>()

        every { testResult.requiresConfirmatoryTest } returns false
        every { evaluateTestResultButtonAction(currentState, newState, testResult) } returns buttonAction
        every { currentState.isActiveIsolation(fixedClock) } returns false
        every { newState.isActiveIsolation(fixedClock) } returns true

        val result = computeTestResultViewStateOnPositive(currentState, newState, testResult)

        assertEquals(PositiveWillBeInIsolation(buttonAction), result)
    }

    @Test
    fun `when new state is active isolation with test that does not require confirmation but supports key submission then return PositiveWillBeInIsolation`() {
        val currentState = mockk<IsolationLogicalState>()
        val newState = mockk<IsolationLogicalState>()

        every { testResult.requiresConfirmatoryTest } returns false
        every { isKeySubmissionSupported(testResult) } returns true
        every { evaluateTestResultButtonAction(currentState, newState, testResult) } returns buttonAction
        every { currentState.isActiveIsolation(fixedClock) } returns false
        every { newState.isActiveIsolation(fixedClock) } returns true

        val result = computeTestResultViewStateOnPositive(currentState, newState, testResult)

        assertEquals(PositiveWillBeInIsolation(buttonAction), result)
    }

    @Test
    fun `when currently isolating with positive confirmed test and staying in isolating due to an indicative test that does not allow to share keys then return PositiveContinueIsolationNoChange`() {
        val currentState = mockk<PossiblyIsolating>()
        val newState = mockk<IsolationLogicalState>()

        every { newState.isActiveIsolation(fixedClock) } returns true
        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns true

        val result = computeTestResultViewStateOnPositive(currentState, newState, testResult)

        assertEquals(PositiveContinueIsolationNoChange, result)
    }

    @Test
    fun `when staying in isolating with a positive indicative completing test that does not support key sharing then return PositiveContinueIsolationNoChange`() {
        val currentState = mockk<IsolationLogicalState>()
        val newState = mockk<PossiblyIsolating>()

        every { currentState.isActiveIsolation(fixedClock) } returns true
        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns false
        every { newState.isActiveIsolation(fixedClock) } returns true
        every { newState.hasCompletedPositiveTestResult() } returns true
        every { evaluateTestResultButtonAction(currentState, newState, testResult) } returns buttonAction

        val result = computeTestResultViewStateOnPositive(currentState, newState, testResult)

        assertEquals(PositiveContinueIsolation(buttonAction), result)
    }

    @Test
    fun `when starting isolation due to a positive indicative completing test that does not support key sharing then return PositiveWillBeInIsolation`() {
        val currentState = mockk<IsolationLogicalState>()
        val newState = mockk<PossiblyIsolating>()

        every { currentState.isActiveIsolation(fixedClock) } returns false
        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns false
        every { newState.isActiveIsolation(fixedClock) } returns true
        every { newState.hasCompletedPositiveTestResult() } returns true
        every { evaluateTestResultButtonAction(currentState, newState, testResult) } returns buttonAction

        val result = computeTestResultViewStateOnPositive(currentState, newState, testResult)

        assertEquals(PositiveWillBeInIsolation(buttonAction), result)
    }

    @Test
    fun `when new state is isolation with positive indicative test that does not support key sharing then return PositiveWillBeInIsolationAndOrderTest`() {
        val currentState = mockk<IsolationLogicalState>()
        val newState = mockk<IsolationLogicalState>()

        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns false
        every { newState.isActiveIsolation(fixedClock) } returns true
        every { newState.hasCompletedPositiveTestResult() } returns false

        val result = computeTestResultViewStateOnPositive(currentState, newState, testResult)

        assertEquals(PositiveWillBeInIsolationAndOrderTest, result)
    }
}
