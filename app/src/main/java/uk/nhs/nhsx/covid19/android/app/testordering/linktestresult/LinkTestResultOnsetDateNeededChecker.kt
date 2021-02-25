package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import javax.inject.Inject

class LinkTestResultOnsetDateNeededChecker @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val relevantTestResultProvider: RelevantTestResultProvider
) {

    fun isInterestedInAskingForSymptomsOnsetDay(testResult: ReceivedTestResult): Boolean {
        val currentState = isolationStateMachine.readState()
        if (testResult.testKitType == LAB_RESULT && testResult.testResult == POSITIVE && !testResult.requiresConfirmatoryTest) {
            val consideredSymptomatic = isConsideredSymptomatic(currentState)
            return !consideredSymptomatic && !relevantTestResultProvider.isTestResultPositive()
        }
        return false
    }

    private fun isConsideredSymptomatic(state: State): Boolean {
        val indexCase = when (state) {
            is Default -> state.previousIsolation?.indexCase
            is Isolation -> state.indexCase
        }
        return indexCase != null && indexCase.selfAssessment && !relevantTestResultProvider.isTestResultNegative()
    }
}
