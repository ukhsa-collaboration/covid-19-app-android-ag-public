package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ComputeTestResultViewStateOnNegativeTest {

    private val currentState = mockk<IsolationLogicalState>()
    private val newState = mockk<PossiblyIsolating>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val computeTestResultViewStateOnNegative = ComputeTestResultViewStateOnNegative(fixedClock)

    @Before
    fun setUp() {
        every { currentState.isActiveIsolation(fixedClock) } returns true
        every { newState.isActiveIsolation(fixedClock) } returns true
    }

    @Test
    fun `when not in active isolation then return NegativeNotInIsolation`() {
        every { currentState.isActiveIsolation(fixedClock) } returns false

        assertEquals(NegativeNotInIsolation, computeTestResultViewStateOnNegative(currentState, newState))
    }

    @Test
    fun `when in active isolation and will be released from isolation then return NegativeWontBeInIsolation`() {
        every { newState.isActiveIsolation(fixedClock) } returns false

        assertEquals(NegativeWontBeInIsolation, computeTestResultViewStateOnNegative(currentState, newState))
    }

    @Test
    fun `when in active isolation, continue isolation and test is being completed then return NegativeWillBeInIsolation`() {
        every { newState.isActiveIndexCase(fixedClock) } returns true
        setUpTestResultIsBeingCompleted()

        assertEquals(NegativeWillBeInIsolation, computeTestResultViewStateOnNegative(currentState, newState))
    }

    @Test
    fun `when in active isolation, continue isolation as active index case and test is not being completed then return NegativeAfterPositiveOrSymptomaticWillBeInIsolation`() {
        every { newState.isActiveIndexCase(fixedClock) } returns true
        setUpTestResultIsNotBeingCompleted()

        assertEquals(
            NegativeAfterPositiveOrSymptomaticWillBeInIsolation,
            computeTestResultViewStateOnNegative(currentState, newState)
        )
    }

    @Test
    fun `when in active isolation, continue isolation without active index case and test is not being completed then return NegativeWillBeInIsolation`() {
        every { newState.isActiveIndexCase(fixedClock) } returns false
        setUpTestResultIsNotBeingCompleted()

        assertEquals(NegativeWillBeInIsolation, computeTestResultViewStateOnNegative(currentState, newState))
    }

    private fun setUpTestResultIsNotBeingCompleted() {
        setUpIsolationStateCompletionStatus(currentState, shouldBeCompleted = true)
        setUpIsolationStateCompletionStatus(newState, shouldBeCompleted = true)
    }

    private fun setUpTestResultIsBeingCompleted() {
        setUpIsolationStateCompletionStatus(currentState, shouldBeCompleted = false)
        setUpIsolationStateCompletionStatus(newState, shouldBeCompleted = true)
    }

    private fun setUpIsolationStateCompletionStatus(isolationLogicalState: IsolationLogicalState, shouldBeCompleted: Boolean) {
        val testResult = mockk<AcknowledgedTestResult>()
        every { isolationLogicalState.getTestResult() } returns testResult
        every { testResult.confirmatoryTestCompletionStatus } returns if (shouldBeCompleted) COMPLETED else null
    }
}
