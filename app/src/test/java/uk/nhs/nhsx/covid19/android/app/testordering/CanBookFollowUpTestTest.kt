package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanBookFollowUpTestTest {

    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)
    private val testResult = mockk<ReceivedTestResult>()

    private val canBookFollowUpTest = CanBookFollowUpTest(fixedClock)

    @Before
    fun setUp() {
        every { testResult.requiresConfirmatoryTest } returns true
    }

    @Test
    fun `when test result does not require confirmatory test then return false`() {
        every { testResult.requiresConfirmatoryTest } returns false

        assertFalse(canBookFollowUpTest(mockk(), mockk(), testResult))
    }

    @Test
    fun `when test result requires confirmatory test and current state is NeverIsolating then return true`() {
        val currentState = mockk<NeverIsolating>()
        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns false

        val newState = mockk<PossiblyIsolating>()
        every { newState.hasCompletedPositiveTestResult() } returns false

        assertTrue(canBookFollowUpTest(currentState, newState, testResult))
    }

    @Test
    fun `when test result requires confirmatory test and is currently isolating due to a positive confirmed test then return false`() {
        val currentState = mockk<PossiblyIsolating>()
        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns true

        assertFalse(canBookFollowUpTest(currentState, mockk(), testResult))
    }

    @Test
    fun `when test result requires confirmatory test and new state is NeverIsolating then return false`() {
        val currentState = mockk<PossiblyIsolating>()
        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns false

        val newState = mockk<NeverIsolating>()
        every { newState.hasCompletedPositiveTestResult() } returns false

        assertTrue(canBookFollowUpTest(currentState, newState, testResult))
    }

    @Test
    fun `when test result requires confirmatory test and new state has a completed positive test then return false`() {
        val currentState = mockk<PossiblyIsolating>()
        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns false

        val newState = mockk<PossiblyIsolating>()
        every { newState.hasCompletedPositiveTestResult() } returns true

        assertFalse(canBookFollowUpTest(currentState, newState, testResult))
    }

    @Test
    fun `when test result requires confirmatory test, not currently isolating with positive confirmed test nor going to be isolating with positive completed test then return true`() {
        val currentState = mockk<PossiblyIsolating>()
        every { currentState.hasActiveConfirmedPositiveTestResult(fixedClock) } returns false

        val newState = mockk<PossiblyIsolating>()
        every { newState.hasCompletedPositiveTestResult() } returns false

        assertTrue(canBookFollowUpTest(currentState, newState, testResult))
    }
}
