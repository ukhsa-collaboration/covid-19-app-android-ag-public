package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import javax.inject.Inject

class CalculateExposureRisk @Inject constructor(
    private val riskScoreCalculatorProvider: RiskScoreCalculatorProvider
) {

    operator fun invoke(
        exposureWindow: ExposureWindow,
        config: RiskScoreCalculatorConfiguration,
        riskCalculation: V2RiskCalculation
    ): Double {
        val scanInstances = exposureWindow.scanInstances
            .filter { it.secondsSinceLastScan > 0 }
            .map { it.toNHSScanInstance() }
        val riskScoreCalculator = riskScoreCalculatorProvider.riskScoreCalculator(config)
        return 60 * riskScoreCalculator.calculate(scanInstances) * exposureWindow.infectiousnessFactor(riskCalculation)
    }

    private fun ScanInstance.toNHSScanInstance() =
        uk.nhs.riskscore.ScanInstance(attenuationValue = minAttenuationDb, secondsSinceLastScan = secondsSinceLastScan)

    private fun ExposureWindow.infectiousnessFactor(riskCalculation: V2RiskCalculation) =
        riskCalculation.infectiousnessWeights.getOrNull(infectiousness)
            ?: riskCalculation.infectiousnessWeights[0]
}
