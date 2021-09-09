package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import java.time.LocalDate
import javax.inject.Inject

class CalculateIndexStartDate @Inject constructor() {

    operator fun invoke(
        selfAssessment: SelfAssessment?,
        testResult: AcknowledgedTestResult?
    ): LocalDate? =
        when {
            selfAssessment != null -> selfAssessment.selfAssessmentDate
            testResult?.isPositive() == true -> testResult.testEndDate
            else -> null
        }
}
