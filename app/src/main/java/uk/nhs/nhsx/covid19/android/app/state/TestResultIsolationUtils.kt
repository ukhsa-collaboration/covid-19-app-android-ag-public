package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Clock
import java.time.LocalDate

fun isTestOlderThanSelfAssessmentSymptoms(
    testResult: ReceivedTestResult,
    currentState: IsolationLogicalState,
    clock: Clock,
    defaultIfNoSymptoms: Boolean = false,
): Boolean =
    currentState.getIndexCase()?.getSelfAssessmentOnsetDate()?.let { onsetDate ->
        testResult.testEndDate(clock).isBefore(onsetDate)
    } ?: defaultIfNoSymptoms

fun createSelfAssessmentFromTestResult(
    receivedTestResult: ReceivedTestResult,
    selfAssessmentDate: LocalDate
): SelfAssessment? =
    if (receivedTestResult.isPositive() && receivedTestResult.symptomsOnsetDate?.explicitDate != null) {
        SelfAssessment(
            selfAssessmentDate = selfAssessmentDate,
            onsetDate = receivedTestResult.symptomsOnsetDate.explicitDate
        )
    } else {
        null
    }
