package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class WouldTestIsolationEndBeforeOrOnStartOfExistingIsolation @Inject constructor(
    private val calculateIndexExpiryDate: CalculateIndexExpiryDate,
    private val clock: Clock
) {

    operator fun invoke(
        receivedTestResult: ReceivedTestResult,
        currentState: IsolationLogicalState,
    ): Boolean =
        when (currentState) {
            is NeverIsolating -> false
            is PossiblyIsolating -> {
                val isolationExpiryDate =
                    getIsolationExpiryDateBasedOnTest(receivedTestResult, currentState.isolationConfiguration)
                isolationExpiryDate?.isBeforeOrEqual(currentState.startDate) ?: false
            }
        }

    private fun getIsolationExpiryDateBasedOnTest(
        testResult: ReceivedTestResult,
        isolationConfiguration: DurationDays
    ): LocalDate? =
        calculateIndexExpiryDate(
            selfAssessment = createSelfAssessmentFromTestResult(testResult, selfAssessmentDate = LocalDate.now(clock)),
            testResult = testResult,
            isolationConfiguration
        )
}
