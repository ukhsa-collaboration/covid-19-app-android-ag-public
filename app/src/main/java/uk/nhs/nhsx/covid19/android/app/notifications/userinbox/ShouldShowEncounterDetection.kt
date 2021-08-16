package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import javax.inject.Inject

class ShouldShowEncounterDetection @Inject constructor(
    private val shouldShowEncounterDetectionActivityProvider: ShouldShowEncounterDetectionActivityProvider,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock
) {
    operator fun invoke(): Boolean {
        invalidateShowEncounterDetectionFlag()
        return shouldShowEncounterDetectionActivityProvider.value == true
    }

    private fun invalidateShowEncounterDetectionFlag() {
        val isInActiveContactIsolation = isolationStateMachine.readLogicalState().isActiveContactCase(clock)
        val exposureToDisplayIsExpired =
            shouldShowEncounterDetectionActivityProvider.value == true && !isInActiveContactIsolation
        if (exposureToDisplayIsExpired) {
            shouldShowEncounterDetectionActivityProvider.value = false
        }
    }
}
