package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.LocalDate
import javax.inject.Inject

class GetRiskyContactEncounterDate @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine
) {
    operator fun invoke(): LocalDate? = isolationStateMachine.readState().contact?.exposureDate
}
