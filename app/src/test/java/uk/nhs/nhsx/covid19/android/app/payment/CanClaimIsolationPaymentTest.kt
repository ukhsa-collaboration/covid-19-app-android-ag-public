package uk.nhs.nhsx.covid19.android.app.payment

import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationStateMachineSetupHelper
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanClaimIsolationPaymentTest : IsolationStateMachineSetupHelper {

    override val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    override val clock = Clock.fixed(Instant.parse("2020-09-01T10:00:00Z"), ZoneOffset.UTC)!!
    private val isolationHelper = IsolationHelper(clock)

    private val testSubject = CanClaimIsolationPayment(
        isolationStateMachine,
        clock
    )

    @Test
    fun `returns false if not in isolation`() {
        givenIsolationState(isolationHelper.neverInIsolation())

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with index case only`() {
        givenIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                selfAssessment = isolationHelper.selfAssessment()
            )
        )

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with contact case and index case and positive acknowledged test since start of isolation`() {
        val isolationStart = LocalDate.now(clock).minusDays(5)
        givenIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contact = isolationHelper.contact(exposureDate = isolationStart),
                testResult = AcknowledgedTestResult(
                    testEndDate = isolationStart.plusDays(1),
                    acknowledgedDate = LocalDate.now(clock),
                    testResult = POSITIVE,
                    testKitType = LAB_RESULT,
                    requiresConfirmatoryTest = false
                )
            )
        )

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns false if in isolation with contact case and no positive tests since start of isolation and contact expired`() {
        givenIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contact = isolationHelper.contact(expired = true)
            )
        )

        val result = testSubject()

        assertFalse(result)
    }

    @Test
    fun `returns true if in isolation with contact case and no positive tests since start of isolation and contact not expired`() {
        givenIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contact = isolationHelper.contact(expired = false)
            )
        )

        val result = testSubject()

        assertTrue(result)
    }

    @Test
    fun `returns true if in isolation with contact case and self-assessment index case, without positive test since start of isolation and contact not expired`() {
        givenIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contact = isolationHelper.contact(expired = false),
                selfAssessment = isolationHelper.selfAssessment(expired = false)
            )
        )

        val result = testSubject()

        assertTrue(result)
    }

    @Test
    fun `returns true if in isolation with contact case and index case without self-assessment, without positive test since start of isolation and contact not expired`() {
        givenIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                contact = isolationHelper.contact(expired = false),
                testResult = AcknowledgedTestResult(
                    testEndDate = LocalDate.now(clock).minusDays(15),
                    acknowledgedDate = LocalDate.now(clock),
                    testResult = POSITIVE,
                    testKitType = LAB_RESULT,
                    requiresConfirmatoryTest = false
                )
            )
        )

        val result = testSubject()

        assertTrue(result)
    }
}
