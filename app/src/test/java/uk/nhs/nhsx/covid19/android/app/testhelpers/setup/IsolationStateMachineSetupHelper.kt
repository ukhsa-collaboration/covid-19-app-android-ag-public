package uk.nhs.nhsx.covid19.android.app.testhelpers.setup

import io.mockk.every
import uk.nhs.nhsx.covid19.android.app.isolation.createIsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock

interface IsolationStateMachineSetupHelper {
    val isolationStateMachine: IsolationStateMachine
    val clock: Clock

    fun givenIsolationState(isolationState: IsolationState) {
        every { isolationStateMachine.readState() } returns isolationState

        val isolationLogicalState = createIsolationLogicalState(clock).invoke(isolationState)
        every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
    }
}
