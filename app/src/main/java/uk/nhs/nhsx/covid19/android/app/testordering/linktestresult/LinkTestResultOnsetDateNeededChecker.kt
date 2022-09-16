package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
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
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider,
    private val clock: Clock
) {

    suspend fun isInterestedInAskingForSymptomsOnsetDay(testResult: ReceivedTestResult): Boolean {
        val currentState = isolationStateMachine.readLogicalState()
        if (localAuthorityPostCodeProvider.requirePostCodeDistrict() == WALES) {
            if (testResult.testResult == POSITIVE && !testResult.requiresConfirmatoryTest) {
                return !currentState.isOrWasActiveIndexCaseAtTimeOfTest(testResult)
            }
        } else if (testResult.testKitType == LAB_RESULT && testResult.testResult == POSITIVE && !testResult.requiresConfirmatoryTest) {
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
