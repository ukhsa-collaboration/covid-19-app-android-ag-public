package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ComputeTestResultViewStateOnVoidTest {

    private val isolationLogicalState = mockk<IsolationLogicalState>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)

    private val computeTestResultViewStateOnVoid = ComputeTestResultViewStateOnVoid(fixedClock)

    @Test
    fun `when in active isolation then return VoidWillBeInIsolation`() {
        every { isolationLogicalState.isActiveIsolation(fixedClock) } returns true

        assertEquals(expected = VoidWillBeInIsolation, computeTestResultViewStateOnVoid(isolationLogicalState))
    }

    @Test
    fun `when not in active isolation then return VoidNotInIsolation`() {
        every { isolationLogicalState.isActiveIsolation(fixedClock) } returns false

        assertEquals(expected = VoidNotInIsolation, computeTestResultViewStateOnVoid(isolationLogicalState))
    }
}
