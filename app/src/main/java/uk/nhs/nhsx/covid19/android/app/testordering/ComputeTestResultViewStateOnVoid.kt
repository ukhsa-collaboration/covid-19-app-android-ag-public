package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation
import java.time.Clock
import javax.inject.Inject

class ComputeTestResultViewStateOnVoid @Inject constructor(
    private val clock: Clock
) {
    operator fun invoke(currentState: IsolationLogicalState): TestResultViewState =
        when {
            currentState.isActiveIsolation(clock) -> VoidWillBeInIsolation
            else -> VoidNotInIsolation
        }
}
