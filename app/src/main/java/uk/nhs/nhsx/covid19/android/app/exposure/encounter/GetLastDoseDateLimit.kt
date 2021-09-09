package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetLastDoseDateLimit @Inject constructor(
    private val getRiskyContactEncounterDate: GetRiskyContactEncounterDate
) {

    operator fun invoke(): LocalDate? = getRiskyContactEncounterDate()?.minus(exposureDoseDateThreshold, ChronoUnit.DAYS)

    companion object {
        private const val exposureDoseDateThreshold = 15L
    }
}
