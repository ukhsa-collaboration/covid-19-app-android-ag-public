package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import javax.inject.Inject

class EvaluateMostRelevantRiskyExposure @Inject constructor() {

    operator fun invoke(exposureWindowsWithRisk: List<ExposureWindowWithRisk>): DayRisk? {
        return exposureWindowsWithRisk
            .maxWithOrNull(compareBy({ it.startOfDayMillis }, { it.calculatedRisk }))
            ?.let { mostRelevantExposureWindow ->
                with(mostRelevantExposureWindow) {
                    DayRisk(startOfDayMillis, calculatedRisk, riskCalculationVersion, matchedKeyCount)
                }
            }
    }
}
