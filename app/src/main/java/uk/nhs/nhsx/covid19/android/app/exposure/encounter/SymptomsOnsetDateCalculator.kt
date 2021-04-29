package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class SymptomsOnsetDateCalculator @Inject constructor(
    val clock: Clock
) {
    fun getMostTrustworthyOnsetDate(testResult: ReceivedTestResult, state: State): LocalDate {
        return state.symptomsOnsetDate ?: symptomsOnsetDateFromTestResult(testResult)
    }

    fun symptomsOnsetDateFromTestResult(testResult: ReceivedTestResult): LocalDate =
        if (testResult.symptomsOnsetDate?.explicitDate != null) {
            testResult.symptomsOnsetDate.explicitDate
        } else {
            val testResultDate = LocalDateTime.ofInstant(testResult.testEndDate, clock.zone).toLocalDate()
            testResultDate.minusDays(INDEX_CASE_ONSET_DATE_BEFORE_TEST_RESULT_DATE)
        }

    companion object {
        private const val INDEX_CASE_ONSET_DATE_BEFORE_TEST_RESULT_DATE: Long = 3
    }
}
