package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import java.time.LocalDate
import kotlin.test.assertEquals

class StateTest {

    @Test
    fun `get assumedOnsetDateForExposureKeys from self-assessment with explicit date`() {
        val selfAssessmentDate = LocalDate.of(2020, 1, 10)
        val onsetDate = LocalDate.of(2020, 1, 3)
        val isolationTrigger = SelfAssessment(selfAssessmentDate, onsetDate)

        assertEquals(onsetDate, isolationTrigger.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from self-assessment without explicit date`() {
        val selfAssessmentDate = LocalDate.of(2020, 1, 10)
        val isolationTrigger = SelfAssessment(selfAssessmentDate)

        assertEquals(LocalDate.of(2020, 1, 8), isolationTrigger.assumedOnsetDateForExposureKeys)
    }

    @Test
    fun `get assumedOnsetDateForExposureKeys from test result`() {
        val testEndDate = LocalDate.of(2020, 1, 10)
        val isolationTrigger = PositiveTestResult(testEndDate)

        assertEquals(LocalDate.of(2020, 1, 7), isolationTrigger.assumedOnsetDateForExposureKeys)
    }
}
