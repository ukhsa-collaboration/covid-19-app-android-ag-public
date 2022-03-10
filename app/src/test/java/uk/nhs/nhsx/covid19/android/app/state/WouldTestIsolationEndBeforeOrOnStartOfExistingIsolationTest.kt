package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WouldTestIsolationEndBeforeOrOnStartOfExistingIsolationTest {

    private val calculateIndexExpiryDate = mockk<CalculateIndexExpiryDate>()

    private val isolationConfiguration = IsolationConfiguration()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-15T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = WouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(
        calculateIndexExpiryDate,
        fixedClock
    )

    @Test
    fun `when never isolating, return false`() {
        val receivedTestResult = givenTestResult()

        val neverIsolating = givenNeverIsolating()

        val testIsolationExpiryDate = LocalDate.now(fixedClock)
        givenTestIsolationExpiryDate(testIsolationExpiryDate)

        val result = testSubject(receivedTestResult, neverIsolating)

        assertFalse(result)
    }

    @Test
    fun `when isolating but test would not trigger isolation, return false`() {
        val receivedTestResult = givenTestResult()

        val isolationStartDate = LocalDate.now(fixedClock)
        val possiblyIsolating = givenIsolatingWithStartDate(isolationStartDate)

        givenTestIsolationExpiryDate(testIsolationExpiryDate = null)

        val result = testSubject(receivedTestResult, possiblyIsolating)

        assertFalse(result)
    }

    @Test
    fun `when isolation from text expires after current isolation start date, return false`() {
        val receivedTestResult = givenTestResult()

        val isolationStartDate = LocalDate.now(fixedClock)
        val possiblyIsolating = givenIsolatingWithStartDate(isolationStartDate)

        val testIsolationExpiryDate = isolationStartDate.plusDays(1)
        givenTestIsolationExpiryDate(testIsolationExpiryDate)

        val result = testSubject(receivedTestResult, possiblyIsolating)

        assertFalse(result)
    }

    @Test
    fun `when isolation from text expires on current isolation start date, return true`() {
        val receivedTestResult = givenTestResult()

        val isolationStartDate = LocalDate.now(fixedClock)
        val possiblyIsolating = givenIsolatingWithStartDate(isolationStartDate)

        givenTestIsolationExpiryDate(testIsolationExpiryDate = isolationStartDate)

        val result = testSubject(receivedTestResult, possiblyIsolating)

        assertTrue(result)
    }

    @Test
    fun `when isolation from text expires after current isolation start date, return true`() {
        val receivedTestResult = givenTestResult()

        val isolationStartDate = LocalDate.now(fixedClock)
        val possiblyIsolating = givenIsolatingWithStartDate(isolationStartDate)

        val testIsolationExpiryDate = isolationStartDate.minusDays(1)
        givenTestIsolationExpiryDate(testIsolationExpiryDate)

        val result = testSubject(receivedTestResult, possiblyIsolating)

        assertTrue(result)
    }

    private fun givenTestResult(): ReceivedTestResult {
        val receivedTestResult = mockk<ReceivedTestResult>()
        every { receivedTestResult.isPositive() } returns false

        return receivedTestResult
    }

    private fun givenTestIsolationExpiryDate(testIsolationExpiryDate: LocalDate?) {
        every { calculateIndexExpiryDate(any(), any(), any()) } returns testIsolationExpiryDate
    }

    private fun givenNeverIsolating(): NeverIsolating =
        NeverIsolating(isolationConfiguration, negativeTest = null)

    private fun givenIsolatingWithStartDate(isolationStartDate: LocalDate): PossiblyIsolating {
        val possiblyIsolating = mockk<PossiblyIsolating>()
        every { possiblyIsolating.isolationConfiguration } returns isolationConfiguration
        every { possiblyIsolating.startDate } returns isolationStartDate

        return possiblyIsolating
    }
}
