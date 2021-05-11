package uk.nhs.nhsx.covid19.android.app.payment

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanClaimIsolationPaymentTest {

    private val stateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-09-01T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject = CanClaimIsolationPayment(
        stateMachine,
        fixedClock
    )

    @Test
    fun `returns false if not in isolation`() {
        every { stateMachine.readLogicalState() } returns isolationHelper.neverInIsolation().asLogical()

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with index case only`() {
        every { stateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                indexInfo = isolationHelper.selfAssessment()
            ).asLogical()

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with contact case and index case and positive acknowledged test since start of isolation`() {
        val isolationStart = LocalDate.now(fixedClock).minusDays(5)
        every { stateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(exposureDate = isolationStart),
                indexInfo = isolationHelper.positiveTest(
                    AcknowledgedTestResult(
                        testEndDate = isolationStart.plusDays(1),
                        acknowledgedDate = LocalDate.now(fixedClock),
                        testResult = POSITIVE,
                        testKitType = LAB_RESULT,
                        requiresConfirmatoryTest = false
                    )
                )
            ).asLogical()

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with contact case and no positive tests since start of isolation and contact expired`() {
        every { stateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(expired = true)
            ).asLogical()

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns true if in isolation with contact case and no positive tests since start of isolation and contact not expired`() {
        every { stateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(expired = false)
            ).asLogical()

        val result = testSubject()

        assertTrue(result)
    }

    @Test
    fun `returns true if in isolation with contact case and self-assessment index case, without positive test since start of isolation and contact not expired`() {
        every { stateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(expired = false),
                indexInfo = isolationHelper.selfAssessment(expired = false)
            ).asLogical()

        val result = testSubject()

        assertTrue(result)
    }

    @Test
    fun `returns true if in isolation with contact case and index case without self-assessment, without positive test since start of isolation and contact not expired`() {
        every { stateMachine.readLogicalState() } returns
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(expired = false),
                indexInfo = isolationHelper.positiveTest(
                    AcknowledgedTestResult(
                        testEndDate = LocalDate.now(fixedClock).minusDays(15),
                        acknowledgedDate = LocalDate.now(fixedClock),
                        testResult = POSITIVE,
                        testKitType = LAB_RESULT,
                        requiresConfirmatoryTest = false
                    )
                )
            ).asLogical()

        val result = testSubject()

        assertTrue(result)
    }
}
