package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.TestResult
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class CalculateIndexExpiryDate @Inject constructor(
    private val clock: Clock
) {

    operator fun invoke(
        selfAssessment: SelfAssessment?,
        testResult: TestResult?,
        isolationConfiguration: IsolationConfiguration
    ): LocalDate? =
        when {
            selfAssessment != null -> {
                when {
                    // The negative test result expired the symptoms
                    testResult?.isNegative() == true -> testResult.testEndDate(clock)

                    // Use explicit onset date as the base
                    selfAssessment.onsetDate != null ->
                        selfAssessment.onsetDate
                            .plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())

                    // Use self-assessment date as the base
                    else ->
                        selfAssessment.selfAssessmentDate
                            .plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong())
                }
            }

            // Use positive test date as the base
            testResult?.isPositive() == true ->
                testResult.testEndDate(clock)
                    .plusDays(isolationConfiguration.indexCaseSinceTestResultEndDate.toLong())

            // No isolation due to index
            else -> null
        }
}
