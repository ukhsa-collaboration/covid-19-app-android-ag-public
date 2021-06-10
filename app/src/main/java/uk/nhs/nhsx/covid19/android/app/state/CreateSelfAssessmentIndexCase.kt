package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import javax.inject.Inject

class CreateSelfAssessmentIndexCase @Inject constructor() {

    operator fun invoke(
        currentState: IsolationLogicalState,
        selfAssessment: SelfAssessment,
        discardTestResultIfPresent: Boolean
    ): IndexCase {
        val isolationConfiguration = currentState.isolationConfiguration
        val potentialIndexExpiryDate =
            if (selfAssessment.onsetDate != null)
                selfAssessment.onsetDate.plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())
            else
                selfAssessment.selfAssessmentDate.plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset.toLong())

        return with(
            IndexCase(
                isolationTrigger = selfAssessment,
                testResult = if (discardTestResultIfPresent) null else currentState.toIsolationState().indexInfo?.testResult,
                expiryDate = potentialIndexExpiryDate
            )
        ) {
            copy(expiryDate = currentState.capExpiryDate(this))
        }
    }
}
