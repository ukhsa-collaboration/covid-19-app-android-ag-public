package uk.nhs.nhsx.covid19.android.app.payment

import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultHandler
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class CanClaimIsolationPayment @Inject constructor(
    private val stateMachine: IsolationStateMachine,
    private val testResultsHandler: TestResultHandler,
    private val clock: Clock
) {

    operator fun invoke(): Boolean {
        val isolationState = stateMachine.readState()
        return isolationState is Isolation &&
            isolationState.isContactCase() &&
            !hasHadPositiveTestSinceStartOfIsolation(isolationState) &&
            !hasContactCaseExpired(isolationState)
    }

    private fun hasHadPositiveTestSinceStartOfIsolation(isolation: Isolation): Boolean =
        testResultsHandler.hasPositiveTestResultAfterOrEqual(isolation.isolationStart)

    private fun hasContactCaseExpired(isolation: Isolation): Boolean {
        return isolation.contactCase?.let { contactCase ->
            LocalDate.now(clock).isAfter(contactCase.expiryDate)
        } ?: true
    }
}
