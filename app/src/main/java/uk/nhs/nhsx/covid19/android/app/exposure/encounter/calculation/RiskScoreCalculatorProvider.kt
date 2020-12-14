package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import uk.nhs.riskscore.RiskScoreCalculator
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import javax.inject.Inject

class RiskScoreCalculatorProvider @Inject constructor() {
    fun riskScoreCalculator(config: RiskScoreCalculatorConfiguration) = RiskScoreCalculator(config)

    fun getRiskCalculationVersion(): Int = 2
}
