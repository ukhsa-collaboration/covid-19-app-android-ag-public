package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import java.time.LocalDate
import kotlin.test.assertEquals

class IndexCaseTest {

    val expiryDate = mockk<LocalDate>()
    val selfAssessmentDate = mockk<LocalDate>()

    @Test
    fun `get assumedOnsetDateForExposureKeys from index case with self-assessment with explicit onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 3)
        val isolationTrigger = SelfAssessment(selfAssessmentDate, onsetDate)
        val indexCase = IndexCase(isolationTrigger, null, expiryDate)

        assertEquals(onsetDate, indexCase.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from index case with self-assessment without explicit onset date`() {
        val selfAssessmentDate = LocalDate.of(2020, 1, 10)
        val isolationTrigger = SelfAssessment(selfAssessmentDate)
        val indexCase = IndexCase(isolationTrigger, null, expiryDate)

        assertEquals(LocalDate.of(2020, 1, 8), indexCase.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from index case with self-assessment with test result end date before onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 7)
        val isolationTrigger = SelfAssessment(selfAssessmentDate, onsetDate)
        val testEndDate = LocalDate.of(2020, 1, 6)
        val testResult = createTestResult(testEndDate)
        val indexCase = IndexCase(isolationTrigger, testResult, expiryDate)

        assertEquals(LocalDate.of(2020, 1, 3), indexCase.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from index case with self-assessment with test result end date equals onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 7)
        val isolationTrigger = SelfAssessment(selfAssessmentDate, onsetDate)
        val testEndDate = LocalDate.of(2020, 1, 7)
        val testResult = createTestResult(testEndDate)
        val indexCase = IndexCase(isolationTrigger, testResult, expiryDate)

        assertEquals(onsetDate, indexCase.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from index case with self-assessment with test result end date after onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 7)
        val isolationTrigger = SelfAssessment(selfAssessmentDate, onsetDate)
        val testEndDate = LocalDate.of(2020, 1, 8)
        val testResult = createTestResult(testEndDate)
        val indexCase = IndexCase(isolationTrigger, testResult, expiryDate)

        assertEquals(onsetDate, indexCase.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from index case with test result`() {
        val testEndDate = LocalDate.of(2020, 1, 10)
        val isolationTrigger = PositiveTestResult(testEndDate)
        val indexCase = IndexCase(isolationTrigger, null, expiryDate)

        assertEquals(LocalDate.of(2020, 1, 7), indexCase.assumedOnsetDateForExposureKeys)
    }

    private fun createTestResult(testEndDate: LocalDate) = AcknowledgedTestResult(
        testEndDate = testEndDate,
        testResult = mockk(),
        testKitType = mockk(),
        acknowledgedDate = mockk()
    )
}
