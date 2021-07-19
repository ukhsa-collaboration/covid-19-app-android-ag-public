package uk.nhs.nhsx.covid19.android.app.payment

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import javax.inject.Inject

class CanClaimIsolationPayment @Inject constructor(
    private val stateMachine: IsolationStateMachine,
    private val clock: Clock
) {

    operator fun invoke(): Boolean {
        val isolationState = stateMachine.readLogicalState()
        return isolationState.isActiveContactCase(clock) &&
            !hasHadPositiveTestSinceStartOfIsolation(isolationState)
    }

    private fun hasHadPositiveTestSinceStartOfIsolation(isolation: IsolationLogicalState): Boolean =
        isolation.getActiveIndexCase(clock)?.testResult?.isPositive() == true
}
