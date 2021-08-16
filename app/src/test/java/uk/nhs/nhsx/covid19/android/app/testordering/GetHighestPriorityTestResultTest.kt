package uk.nhs.nhsx.covid19.android.app.testordering

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.FoundTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.GetHighestPriorityTestResult.HighestPriorityTestResult.NoTestResult
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetHighestPriorityTestResultTest {
    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>()

    private val getHighestPriorityTestResult = GetHighestPriorityTestResult(unacknowledgedTestResultsProvider)

    @Test
    fun `when no unacknowledged test results stored then return NoTestResult`() {
        every { unacknowledgedTestResultsProvider.testResults } returns listOf()

        assertEquals(expected = NoTestResult, getHighestPriorityTestResult())
    }

    @Test
    fun `when unacknowledged test results of all types are stored then return positive test result`() {
        every { unacknowledgedTestResultsProvider.testResults } returns getTestResults(VOID, NEGATIVE, PLOD, POSITIVE)

        val result = getHighestPriorityTestResult()

        assertTrue(result is FoundTestResult)
        assertEquals(expected = POSITIVE, result.testResult.testResult)
    }

    @Test
    fun `when plod, void and negative unacknowledged test results stored then return plod test result`() {
        every { unacknowledgedTestResultsProvider.testResults } returns getTestResults(NEGATIVE, VOID, PLOD)

        val result = getHighestPriorityTestResult()

        assertTrue(result is FoundTestResult)
        assertEquals(expected = PLOD, result.testResult.testResult)
    }

    @Test
    fun `when void and negative unacknowledged test results stored then return negative test result`() {
        every { unacknowledgedTestResultsProvider.testResults } returns getTestResults(VOID, NEGATIVE)

        val result = getHighestPriorityTestResult()

        assertTrue(result is FoundTestResult)
        assertEquals(expected = NEGATIVE, result.testResult.testResult)
    }

    @Test
    fun `when only void unacknowledged test result stored then return void test result`() {
        every { unacknowledgedTestResultsProvider.testResults } returns getTestResults(VOID)

        val result = getHighestPriorityTestResult()

        assertTrue(result is FoundTestResult)
        assertEquals(expected = VOID, result.testResult.testResult)
    }

    private fun getTestResults(vararg testResult: VirologyTestResult): List<ReceivedTestResult> =
        testResult.map { createTestResultMock(it) }

    private fun createTestResultMock(testResult: VirologyTestResult): ReceivedTestResult {
        val testResultMock = mockk<ReceivedTestResult>()
        every { testResultMock.testResult } returns testResult
        return testResultMock
    }
}
