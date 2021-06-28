package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestResultTest {
    private val fixedClock = Clock.fixed(Instant.parse("2021-06-07T10:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.now(fixedClock)

    @Test
    fun `test isOlderThan`() {
        val olderTestResult = createAcknowledgedTestResult(today.minusDays(1))
        val testResult = createAcknowledgedTestResult(today)
        val newerTestResult = createAcknowledgedTestResult(today.plusDays(1))

        assertTrue(olderTestResult.isOlderThan(testResult, fixedClock))
        assertTrue(olderTestResult.isOlderThan(newerTestResult, fixedClock))
        assertTrue(testResult.isOlderThan(newerTestResult, fixedClock))
        assertFalse(olderTestResult.isOlderThan(olderTestResult, fixedClock))
        assertFalse(testResult.isOlderThan(olderTestResult, fixedClock))
        assertFalse(newerTestResult.isOlderThan(olderTestResult, fixedClock))
    }

    @Test
    fun `given test with positive confirmatory day limit`() {
        val testSubject = createAcknowledgedTestResult(today, confirmatoryDayLimit = 3)

        assertFalse(testSubject.isDateWithinConfirmatoryDayLimit(today.minusDays(1), fixedClock))
        assertTrue(testSubject.isDateWithinConfirmatoryDayLimit(today, fixedClock))
        assertTrue(testSubject.isDateWithinConfirmatoryDayLimit(today.plusDays(1), fixedClock))
        assertTrue(testSubject.isDateWithinConfirmatoryDayLimit(today.plusDays(2), fixedClock))
        assertTrue(testSubject.isDateWithinConfirmatoryDayLimit(today.plusDays(3), fixedClock))
        assertFalse(testSubject.isDateWithinConfirmatoryDayLimit(today.plusDays(4), fixedClock))
    }

    @Test
    fun `given test with negative confirmatory day limit`() {
        val testSubject = createAcknowledgedTestResult(today, confirmatoryDayLimit = -1)

        assertFalse(testSubject.isDateWithinConfirmatoryDayLimit(today.minusDays(1), fixedClock))
        assertFalse(testSubject.isDateWithinConfirmatoryDayLimit(today, fixedClock))
        assertFalse(testSubject.isDateWithinConfirmatoryDayLimit(today.plusDays(1), fixedClock))
        assertFalse(testSubject.isDateWithinConfirmatoryDayLimit(today.plusDays(3), fixedClock))
    }

    @Test
    fun `given test with null confirmatory day limit`() {
        val testSubject = createAcknowledgedTestResult(today, confirmatoryDayLimit = null)

        assertTrue(testSubject.isDateWithinConfirmatoryDayLimit(today.minusDays(1), fixedClock))
        assertTrue(testSubject.isDateWithinConfirmatoryDayLimit(today, fixedClock))
        assertTrue(testSubject.isDateWithinConfirmatoryDayLimit(today.plusDays(1), fixedClock))
        assertTrue(testSubject.isDateWithinConfirmatoryDayLimit(today.plusDays(3), fixedClock))
    }

    private fun createAcknowledgedTestResult(testEndDate: LocalDate, confirmatoryDayLimit: Int? = null) = AcknowledgedTestResult(
        testEndDate = testEndDate,
        testResult = POSITIVE,
        testKitType = RAPID_RESULT,
        acknowledgedDate = testEndDate.plusDays(1),
        requiresConfirmatoryTest = true,
        confirmatoryDayLimit = confirmatoryDayLimit,
    )
}
