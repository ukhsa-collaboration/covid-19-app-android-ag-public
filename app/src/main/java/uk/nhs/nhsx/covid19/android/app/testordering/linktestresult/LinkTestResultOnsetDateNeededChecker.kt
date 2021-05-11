package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import javax.inject.Inject

class LinkTestResultOnsetDateNeededChecker @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine
) {

    fun isInterestedInAskingForSymptomsOnsetDay(testResult: ReceivedTestResult): Boolean {
        val currentState = isolationStateMachine.readLogicalState()
        if (testResult.testKitType == LAB_RESULT && testResult.testResult == POSITIVE && !testResult.requiresConfirmatoryTest) {
            val consideredSymptomatic = currentState.isConsideredSymptomatic()
            return !consideredSymptomatic && !currentState.hasPositiveTestResult()
        }
        return false
    }

    private fun IsolationLogicalState.isConsideredSymptomatic(): Boolean =
        this is PossiblyIsolating &&
            indexInfo is IndexCase &&
            indexInfo.isSelfAssessment() &&
            indexInfo.testResult?.testResult != RelevantVirologyTestResult.NEGATIVE

    private fun IsolationLogicalState.hasPositiveTestResult(): Boolean =
        this is PossiblyIsolating &&
            indexInfo != null &&
            indexInfo.testResult?.testResult == RelevantVirologyTestResult.POSITIVE
}
