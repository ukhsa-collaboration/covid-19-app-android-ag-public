package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.UnknownExposureDate
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.WalesWithinAdviceWindow
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class EvaluateTestingAdviceToShow @Inject constructor(
    private val getRiskyContactEncounterDate: GetRiskyContactEncounterDate,
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) {
    suspend operator fun invoke(clock: Clock) =
        getRiskyContactEncounterDate()?.let { encounterDate ->
            if (isWithinWelshExtendedAdviceWindow(encounterDate, clock)) {
                WalesWithinAdviceWindow(date = encounterDate.plusDays(ADVISED_PCR_DATE_OFFSET))
            } else {
                Default
            }
        } ?: UnknownExposureDate

    private suspend fun isWithinWelshExtendedAdviceWindow(encounterDate: LocalDate, clock: Clock) =
        localAuthorityPostCodeProvider.getPostCodeDistrict() == WALES &&
                withinExtendedAdviceWindow(encounterDate, clock)

    private fun withinExtendedAdviceWindow(encounterDate: LocalDate, clock: Clock): Boolean {
        return LocalDate.now(clock).isBeforeOrEqual(encounterDate.plusDays(EXTENDED_ADVICE_WINDOW_DAY_LIMIT))
    }

    companion object {
        private const val EXTENDED_ADVICE_WINDOW_DAY_LIMIT = 5L
        private const val ADVISED_PCR_DATE_OFFSET = 8L
    }

    sealed class TestingAdviceToShow {
        object Default : TestingAdviceToShow()
        data class WalesWithinAdviceWindow(val date: LocalDate) : TestingAdviceToShow()
        object UnknownExposureDate : TestingAdviceToShow()
    }
}
