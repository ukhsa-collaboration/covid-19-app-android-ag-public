package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.mockk
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IsolationStateTest {

    val selfAssessmentDate = mockk<LocalDate>()

    @Test
    fun `get assumedOnsetDateForExposureKeys from isolation state with self-assessment with explicit onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 3)
        val selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate)
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            selfAssessment = selfAssessment
        )

        assertEquals(onsetDate, isolationState.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from isolation state with self-assessment without explicit onset date`() {
        val selfAssessmentDate = LocalDate.of(2020, 1, 10)
        val selfAssessment = SelfAssessment(selfAssessmentDate)
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            selfAssessment = selfAssessment
        )

        assertEquals(LocalDate.of(2020, 1, 8), isolationState.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from isolation state with self-assessment with positive test result end date before onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 7)
        val selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate)
        val testEndDate = LocalDate.of(2020, 1, 6)
        val testResult = createTestResult(testEndDate, POSITIVE)
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            selfAssessment = selfAssessment,
            testResult = testResult
        )

        assertEquals(LocalDate.of(2020, 1, 6), isolationState.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from isolation state with self-assessment with positive test result end date equals onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 7)
        val selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate)
        val testEndDate = LocalDate.of(2020, 1, 7)
        val testResult = createTestResult(testEndDate, POSITIVE)
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            selfAssessment = selfAssessment,
            testResult = testResult
        )

        assertEquals(onsetDate, isolationState.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from isolation state with self-assessment with positive test result end date after onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 7)
        val selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate)
        val testEndDate = LocalDate.of(2020, 1, 8)
        val testResult = createTestResult(testEndDate, POSITIVE)
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            selfAssessment = selfAssessment,
            testResult = testResult
        )

        assertEquals(onsetDate, isolationState.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from isolation state with positive test result`() {
        val testEndDate = LocalDate.of(2020, 1, 10)
        val testResult = createTestResult(testEndDate, POSITIVE)
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            testResult = testResult
        )

        assertEquals(LocalDate.of(2020, 1, 10), isolationState.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from isolation state with negative test result`() {
        val testEndDate = LocalDate.of(2020, 1, 10)
        val testResult = createTestResult(testEndDate, NEGATIVE)
        val isolationState = IsolationState(
            isolationConfiguration = DurationDays(),
            testResult = testResult
        )

        assertNull(isolationState.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDate from self-assessment with explicit onset date`() {
        val onsetDate = LocalDate.of(2020, 1, 3)
        val selfAssessment = SelfAssessment(selfAssessmentDate, onsetDate)

        assertEquals(onsetDate, selfAssessment.assumedOnsetDate)
    }

    @Test
    fun `get assumedOnsetDate from self-assessment without explicit onset date`() {
        val selfAssessmentDate = LocalDate.of(2020, 1, 10)
        val selfAssessment = SelfAssessment(selfAssessmentDate)

        assertEquals(LocalDate.of(2020, 1, 8), selfAssessment.assumedOnsetDate)
    }

    private fun createTestResult(
        testEndDate: LocalDate,
        testResult: RelevantVirologyTestResult
    ) = AcknowledgedTestResult(
        testEndDate = testEndDate,
        testResult = testResult,
        testKitType = mockk(),
        acknowledgedDate = mockk()
    )
}
