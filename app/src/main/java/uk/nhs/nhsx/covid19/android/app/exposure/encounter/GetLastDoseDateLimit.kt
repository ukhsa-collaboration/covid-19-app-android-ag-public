package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetLastDoseDateLimit @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine
) {

    operator fun invoke(): LocalDate =
        isolationStateMachine.readState().contactCase?.let {
            it.exposureDate.minus(exposureDoseDateThreshold, ChronoUnit.DAYS)
        } ?: throw IllegalStateException("No contact case exists")

    companion object {
        private const val exposureDoseDateThreshold = 15L
    }
}
