package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
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

fun wouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(
    receivedTestResult: ReceivedTestResult,
    currentState: IsolationLogicalState,
    clock: Clock
): Boolean =
    when (currentState) {
        is NeverIsolating -> false
        is PossiblyIsolating -> {
            val isolationExpiryDate =
                getIsolationExpiryDateBasedOnTest(receivedTestResult, currentState.isolationConfiguration, clock)
            isolationExpiryDate.isBeforeOrEqual(currentState.startDate)
        }
    }

fun getIsolationExpiryDateBasedOnTest(
    testResult: ReceivedTestResult,
    isolationConfiguration: DurationDays,
    clock: Clock
): LocalDate {
    return if (testResult.symptomsOnsetDate?.explicitDate != null) {
        testResult.symptomsOnsetDate.explicitDate
            .plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())
    } else {
        getIsolationExpiryDateBasedOnTestEndDate(testResult.testEndDate(clock), isolationConfiguration)
    }
}

fun getIsolationExpiryDateBasedOnTestEndDate(
    testEndDate: LocalDate,
    isolationConfiguration: DurationDays
): LocalDate =
    testEndDate.plusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong())
