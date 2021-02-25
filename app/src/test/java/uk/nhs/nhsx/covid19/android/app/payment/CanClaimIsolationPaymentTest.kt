package uk.nhs.nhsx.covid19.android.app.payment

import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultHandler

class CanClaimIsolationPaymentTest {

    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val testResultHandler = mockk<TestResultHandler>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-09-01T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = CanClaimIsolationPayment(
        stateMachine,
        testResultHandler,
        fixedClock
    )

    @Test
    fun `returns false if not in isolation`() {
        every { stateMachine.readState() } returns Default()

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with index case only`() {
        every { stateMachine.readState() } returns Isolation(
            isolationStart = Instant.now(fixedClock).minus(5, DAYS),
            isolationConfiguration = DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(30),
                selfAssessment = false
            )
        )

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with contact case and positive test since start of isolation`() {
        val isolationStart = Instant.now(fixedClock).minus(5, DAYS)

        every { stateMachine.readState() } returns Isolation(
            isolationStart = isolationStart,
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(30)
            )
        )

        every { testResultHandler.hasTestResultMatching(any()) } returns true

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with contact case and no positive tests since start of isolation and contact expired`() {
        every { stateMachine.readState() } returns Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).minusDays(1)
            )
        )

        every { testResultHandler.hasTestResultMatching(any()) } returns false

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns true if in isolation with contact case and no positive tests since start of isolation and contact not expired`() {
        every { stateMachine.readState() } returns Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            )
        )

        every { testResultHandler.hasTestResultMatching(any()) } returns false

        val result = testSubject()

        assertTrue(result)
    }

    @Test
    fun `returns true if in isolation with contact case and self-assessment index case, without positive test since start of isolation and contact not expired`() {
        every { stateMachine.readState() } returns Isolation(
            isolationStart = Instant.now(fixedClock).minus(20, DAYS),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5),
                selfAssessment = true
            )
        )

        every { testResultHandler.hasTestResultMatching(any()) } returns false

        val result = testSubject()

        assertTrue(result)
    }

    @Test
    fun `returns true if in isolation with contact case and index case without self-assessment, without positive test since start of isolation and contact not expired`() {
        every { stateMachine.readState() } returns Isolation(
            isolationStart = Instant.now(fixedClock).minus(20, DAYS),
            isolationConfiguration = DurationDays(),
            contactCase = ContactCase(
                startDate = Instant.now(fixedClock),
                notificationDate = null,
                expiryDate = LocalDate.now(fixedClock).plusDays(1)
            ),
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(fixedClock),
                expiryDate = LocalDate.now(fixedClock).plusDays(5),
                selfAssessment = false
            )
        )

        every { testResultHandler.hasTestResultMatching(any()) } returns false

        val result = testSubject()

        assertTrue(result)
    }
}
