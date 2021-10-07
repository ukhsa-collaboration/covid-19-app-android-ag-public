package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.GetRiskyContactEncounterDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class GetAgeLimitBeforeEncounter @Inject constructor(
    private val getRiskyContactEncounterDate: GetRiskyContactEncounterDate,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) {

    suspend operator fun invoke(): LocalDate? = withContext(Dispatchers.Default) {
        getRiskyContactEncounterDate()?.let {
            if (localAuthorityPostCodeProvider.getPostCodeDistrict() == ENGLAND) {
                it.minus(AGE_LIMIT_THRESHOLD, ChronoUnit.DAYS)
            } else {
                it
            }
        }
    }

    companion object {
        private const val AGE_LIMIT_THRESHOLD = 183L
    }
}
