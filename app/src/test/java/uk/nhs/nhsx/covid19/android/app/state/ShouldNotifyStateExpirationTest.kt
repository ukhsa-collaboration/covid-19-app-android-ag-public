package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.DoNotNotify
import uk.nhs.nhsx.covid19.android.app.state.ShouldNotifyStateExpiration.ShouldNotifyStateExpirationResult.Notify
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ShouldNotifyStateExpirationTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val calculateExpirationNotificationTime = mockk<CalculateExpirationNotificationTime>()
    private val now = Instant.parse("2020-05-21T10:00:00Z")
    private val fixedClock = Clock.fixed(now, ZoneOffset.UTC)
    private val isolationHelper = IsolationLogicalHelper(fixedClock)

    private val shouldNotifyStateExpiration = ShouldNotifyStateExpiration(
        isolationStateMachine,
        calculateExpirationNotificationTime,
        fixedClock
    )

    @Test
    fun `do not notify when never isolating`() = runBlocking {
        every { isolationStateMachine.readLogicalState() } returns isolationHelper.neverInIsolation()

        val result = shouldNotifyStateExpiration()

        assertEquals(DoNotNotify, result)
    }

    @Test
    fun `do not notify when possibly isolating and expiry already acknowledged`() = runBlocking {
        val expiryDate = LocalDate.of(2020, 5, 20)
        setIsolation(expiryDate, hasAcknowledgedEndOfIsolation = true)

        val result = shouldNotifyStateExpiration()

        assertEquals(DoNotNotify, result)
    }

    @Test
    fun `notify when possibly isolating, expiry not acknowledge, and now equals notification time`() = runBlocking {
        val expiryDate = LocalDate.of(2020, 5, 22)
        setIsolation(expiryDate, hasAcknowledgedEndOfIsolation = false)
        every { calculateExpirationNotificationTime(expiryDate) } returns now

        val result = shouldNotifyStateExpiration()

        assertEquals(Notify(expiryDate), result)
    }

    @Test
    fun `notify when possibly isolating, expiry not acknowledged, and now after notification time`() = runBlocking {
        val expiryDate = LocalDate.of(2020, 5, 22)
        setIsolation(expiryDate, hasAcknowledgedEndOfIsolation = false)
        every { calculateExpirationNotificationTime(expiryDate) } returns now.minusSeconds(1)

        val result = shouldNotifyStateExpiration()

        assertEquals(Notify(expiryDate), result)
    }

    @Test
    fun `do not notify when possibly isolating, expiry not acknowledged, and now before notification time`() = runBlocking {
        val expiryDate = LocalDate.of(2020, 5, 23)
        setIsolation(expiryDate, hasAcknowledgedEndOfIsolation = false)
        every { calculateExpirationNotificationTime(expiryDate) } returns now.plusSeconds(1)

        val result = shouldNotifyStateExpiration()

        assertEquals(DoNotNotify, result)
    }

    private fun setIsolation(expiryDate: LocalDate, hasAcknowledgedEndOfIsolation: Boolean) {
        every { isolationStateMachine.readLogicalState() } returns
            isolationHelper.selfAssessment(
                selfAssessmentDate = LocalDate.now(fixedClock),
                expiryDate = expiryDate
            ).asIsolation(hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation)
    }
}
