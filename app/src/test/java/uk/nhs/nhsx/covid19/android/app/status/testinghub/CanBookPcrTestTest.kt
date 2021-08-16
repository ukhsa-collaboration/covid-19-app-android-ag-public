package uk.nhs.nhsx.covid19.android.app.status.testinghub

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanBookPcrTestTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val lastVisitedBookTestTypeVenueDateProvider = mockk<LastVisitedBookTestTypeVenueDateProvider>()
    private val fixedClock = Clock.fixed(Instant.parse("2021-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val isolationLogicalState = mockk<IsolationLogicalState>()

    private val canBookPcrTest = CanBookPcrTest(
        isolationStateMachine,
        lastVisitedBookTestTypeVenueDateProvider,
        fixedClock
    )

    @Before
    fun setUp() {
        every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
        givenHasNoBookTestTypeVenueAtRisk()
        givenHasNoActiveIsolation()
    }

    @Test
    fun `when has no book test type risky venue and user is not in active isolation then return false`() {
        assertFalse(canBookPcrTest())
    }

    @Test
    fun `when has book test type risky venue and user is not in active isolation then return true`() {
        givenHasBookTestTypeVenueAtRisk()

        assertTrue(canBookPcrTest())
    }

    @Test
    fun `when has no book test type risky venue and user is in active isolation then return true`() {
        givenHasActiveIsolation()

        assertTrue(canBookPcrTest())
    }

    @Test
    fun `when has book test type risky venue and user is in active isolation then return true`() = runBlocking {
        givenHasBookTestTypeVenueAtRisk()
        givenHasActiveIsolation()

        assertTrue(canBookPcrTest())
    }

    private fun givenHasBookTestTypeVenueAtRisk() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true
    }

    private fun givenHasNoBookTestTypeVenueAtRisk() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false
    }

    private fun givenHasActiveIsolation() {
        every { isolationLogicalState.isActiveIsolation(fixedClock) } returns true
    }

    private fun givenHasNoActiveIsolation() {
        every { isolationLogicalState.isActiveIsolation(fixedClock) } returns false
    }
}
