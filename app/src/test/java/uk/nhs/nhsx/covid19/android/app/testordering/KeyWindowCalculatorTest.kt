package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.DateWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.TestResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class KeyWindowCalculatorTest {

    private val stateMachine = mockk<IsolationStateMachine>()
    private val clock = Clock.fixed(Instant.parse("2020-07-26T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = KeyWindowCalculator(stateMachine, clock)

    @Test
    fun `user is still in isolation`() {
        val symptomsOnsetDate = LocalDate.parse("2020-07-20")
        val indexCase = Isolation(
            isolationStart = Instant.parse("2020-07-21T12:00:00Z"),
            expiryDate = LocalDate.parse("2020-07-27"),
            indexCase = IndexCase(
                symptomsOnsetDate,
                testResult = TestResult(
                    testEndDate = Instant.parse("2020-07-25T12:00:00Z"),
                    result = POSITIVE
                )
            )
        )
        every { stateMachine.readState(any()) } returns indexCase

        val actualDateWindow = testSubject.calculateDateWindow()

        val expectedDateWindow = DateWindow(
            fromInclusive = symptomsOnsetDate.minusDays(2),
            toInclusive = LocalDate.parse("2020-07-25")
        )
        assertEquals(expectedDateWindow, actualDateWindow)
    }

    @Test
    fun `submits keys for specific date window if user is no longer in isolation`() {
        val symptomsOnsetDate = LocalDate.parse("2020-07-20")
        val testEndDate = Instant.parse("2020-07-25T12:00:00Z")
        val indexCase = Isolation(
            isolationStart = Instant.parse("2020-07-21T12:00:00Z"),
            expiryDate = LocalDate.parse("2020-07-29"),
            indexCase = IndexCase(
                symptomsOnsetDate,
                testResult = TestResult(
                    testEndDate = testEndDate,
                    result = POSITIVE
                )
            )
        )
        every { stateMachine.readState(any()) } returns Default(previousIsolation = indexCase)

        val actualDateWindow =
            testSubject.calculateDateWindow()

        val expectedDateWindow = DateWindow(
            fromInclusive = TestResultViewModelTest.symptomsOnsetDate.minusDays(2),
            toInclusive = LocalDate.now(clock).minusDays(1)
        )
        assertEquals(expectedDateWindow, actualDateWindow)
    }
}
