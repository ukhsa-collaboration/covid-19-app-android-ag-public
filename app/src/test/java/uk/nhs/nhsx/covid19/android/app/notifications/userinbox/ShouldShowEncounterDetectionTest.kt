package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldShowEncounterDetectionTest {

    private val shouldShowEncounterDetectionActivityProvider =
        mockk<ShouldShowEncounterDetectionActivityProvider>(relaxUnitFun = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val isolationLogicalState = mockk<IsolationLogicalState>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-22T10:00:00Z"), ZoneOffset.UTC)

    private val shouldShowEncounterDetection = ShouldShowEncounterDetection(
        shouldShowEncounterDetectionActivityProvider,
        isolationStateMachine,
        fixedClock
    )

    @Before
    fun setUp() {
        every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
    }

    @Test
    fun `when shouldShowEncounterDetectionActivity flag is false then return false`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns false
        every { isolationLogicalState.isActiveContactCase(fixedClock) } returns true

        assertFalse(shouldShowEncounterDetection())

        verify(exactly = 0) { shouldShowEncounterDetectionActivityProvider setProperty "value" value eq(false) }
    }

    @Test
    fun `when contact case isolation is expired as ShouldShowEncounterDetection is invoked then reset flag and return false`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns true andThen false
        every { isolationLogicalState.isActiveContactCase(fixedClock) } returns false

        assertFalse(shouldShowEncounterDetection())

        verify { shouldShowEncounterDetectionActivityProvider setProperty "value" value eq(false) }
    }

    @Test
    fun `when in active contact case isolation as ShouldShowEncounterDetection is invoked then return true`() {
        every { shouldShowEncounterDetectionActivityProvider.value } returns true
        every { isolationLogicalState.isActiveContactCase(fixedClock) } returns true

        assertTrue(shouldShowEncounterDetection())

        verify(exactly = 0) { shouldShowEncounterDetectionActivityProvider setProperty "value" value eq(false) }
    }
}
