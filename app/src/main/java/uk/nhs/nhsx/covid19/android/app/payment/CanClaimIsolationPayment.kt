package uk.nhs.nhsx.covid19.android.app.payment

import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class CanClaimIsolationPayment @Inject constructor(
    private val stateMachine: IsolationStateMachine,
    private val testResultsProvider: TestResultsProvider,
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
        testResultsProvider.hasHadPositiveTestSince(isolation.isolationStart)

    private fun hasContactCaseExpired(isolation: Isolation): Boolean {
        return isolation.contactCase?.let { contactCase ->
            LocalDate.now(clock).isAfter(contactCase.expiryDate)
        } ?: true
    }
}
