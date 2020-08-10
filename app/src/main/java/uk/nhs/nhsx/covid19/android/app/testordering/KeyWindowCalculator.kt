package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.DateWindow
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class KeyWindowCalculator(
    private val stateMachine: IsolationStateMachine,
    private val clock: Clock
) {
    @Inject
    constructor(stateMachine: IsolationStateMachine) : this(stateMachine, clock = Clock.systemUTC())

    fun calculateDateWindow(): DateWindow? {
        val state = stateMachine.readState()
        val latestIsolation = when (state) {
            is Isolation -> state
            is Default -> state.previousIsolation
        } ?: return null
        val indexCase = latestIsolation.indexCase ?: return null
        val fromInclusive = indexCase.symptomsOnsetDate.minusDays(2)
        val isInIsolation = state is Isolation

        val yesterday = LocalDate.now(clock).minusDays(1)
        val toInclusive = if (isInIsolation) {
            yesterday
        } else {
            latestIsolation.expiryDate
        } ?: return null

        val restrictedToInclusive = if (toInclusive.isBefore(yesterday)) {
            toInclusive
        } else {
            yesterday
        }
        return DateWindow(
            fromInclusive,
            restrictedToInclusive
        )
    }
}
