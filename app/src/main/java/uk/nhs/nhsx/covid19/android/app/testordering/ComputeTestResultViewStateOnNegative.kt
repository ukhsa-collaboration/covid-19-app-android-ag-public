package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import java.time.Clock
import javax.inject.Inject

class ComputeTestResultViewStateOnNegative @Inject constructor(
    private val clock: Clock
) {
    operator fun invoke(currentState: IsolationLogicalState, newState: IsolationLogicalState): TestResultViewState {
        val isInActiveIsolation = currentState.isActiveIsolation(clock)
        return if (isInActiveIsolation) {
            val willContinueToIsolate = newState.isActiveIsolation(clock)
            if (willContinueToIsolate) {
                if (isTestResultBeingCompleted(currentState, newState))
                    NegativeWillBeInIsolation
                else if (newState is PossiblyIsolating && newState.isActiveIndexCase(clock))
                    NegativeAfterPositiveOrSymptomaticWillBeInIsolation
                else
                    NegativeWillBeInIsolation
            } else
                NegativeWontBeInIsolation
        } else {
            NegativeNotInIsolation
        }
    }

    private fun isTestResultBeingCompleted(
        currentState: IsolationLogicalState,
        newState: IsolationLogicalState
    ): Boolean {
        val currentStateTestResultIsNotCompleted =
            currentState.toIsolationState().indexInfo?.testResult?.confirmatoryTestCompletionStatus == null
        val newStateTestResultIsCompleted =
            newState.toIsolationState().indexInfo?.testResult?.confirmatoryTestCompletionStatus != null
        return currentStateTestResultIsNotCompleted && newStateTestResultIsCompleted
    }
}
