package uk.nhs.nhsx.covid19.android.app.status.testinghub

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationStateMachineSetupHelper
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanBookPcrTestTest : IsolationStateMachineSetupHelper {

    override val isolationStateMachine = mockk<IsolationStateMachine>()
    private val lastVisitedBookTestTypeVenueDateProvider = mockk<LastVisitedBookTestTypeVenueDateProvider>()
    override val clock = Clock.fixed(Instant.parse("2021-01-01T00:00:00Z"), ZoneOffset.UTC)!!
    private val isolationHelper = IsolationHelper(clock)

    private val canBookPcrTest = CanBookPcrTest(
        isolationStateMachine,
        lastVisitedBookTestTypeVenueDateProvider,
        clock
    )

    @Before
    fun setUp() {
        givenHasNoBookTestTypeVenueAtRisk()
        givenNeverInIsolation()
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

    @Test
    fun `when user is exempt from isolating but has been had an encounter date within last 10 days show then return true`() = runBlocking {
        givenIsExemptFromIsolationAfterContactNotification(10)

        assertTrue(canBookPcrTest())
    }

    @Test
    fun `when user is exempt from isolating but has been had an encounter date within last 11 days show then return false`() = runBlocking {
        givenIsExemptFromIsolationAfterContactNotification(11)

        assertFalse(canBookPcrTest())
    }

    private fun givenNeverInIsolation() {
        givenIsolationState(isolationHelper.neverInIsolation())
    }

    private fun givenIsExemptFromIsolationAfterContactNotification(numDays: Long) {
        val exposureDate = LocalDate.now(clock).minusDays(numDays)

        givenIsolationState(
            isolationHelper.contactWithOptOutDate(
                exposureDate = exposureDate,
                optOutOfContactIsolation = exposureDate
            ).asIsolation()
        )
    }

    private fun givenHasActiveIsolation() {
        val onsetDate = LocalDate.now(clock).minusDays(1)
        givenIsolationState(isolationHelper.selfAssessment(onsetDate = onsetDate).asIsolation())
    }

    private fun givenHasBookTestTypeVenueAtRisk() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true
    }

    private fun givenHasNoBookTestTypeVenueAtRisk() {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false
    }
}
