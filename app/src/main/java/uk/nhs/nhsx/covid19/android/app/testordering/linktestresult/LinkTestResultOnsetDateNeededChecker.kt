package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Clock
import javax.inject.Inject

class LinkTestResultOnsetDateNeededChecker @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock
) {

    fun isInterestedInAskingForSymptomsOnsetDay(testResult: ReceivedTestResult): Boolean {
        val currentState = isolationStateMachine.readLogicalState()
        if (testResult.testResult == POSITIVE && !testResult.requiresConfirmatoryTest) {
            return !currentState.isOrWasActiveIndexCaseAtTimeOfTest(testResult)
        }
        return false
    }

    private fun IsolationLogicalState.isOrWasActiveIndexCaseAtTimeOfTest(testResult: ReceivedTestResult): Boolean =
        this is PossiblyIsolating &&
                indexInfo != null &&
                indexInfo is IndexCase &&
                testResult.testEndDate(clock).isBefore(indexInfo.expiryDate)
}
