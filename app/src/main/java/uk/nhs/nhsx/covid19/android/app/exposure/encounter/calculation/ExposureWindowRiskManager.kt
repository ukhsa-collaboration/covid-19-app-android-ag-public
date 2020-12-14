package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.DiagnosisKeysDataMapping
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.ObservationType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.ObservationType.gen
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.ObservationType.log
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation
import uk.nhs.riskscore.InitialData
import uk.nhs.riskscore.PowerLossParameters
import uk.nhs.riskscore.RiskScoreCalculatorConfiguration
import uk.nhs.riskscore.RssiParameters
import uk.nhs.riskscore.SmootherParameters
import javax.inject.Inject
import uk.nhs.riskscore.ObservationType as RiskScoreObservationType

class ExposureWindowRiskManager @Inject constructor(
    private val exposureNotificationApi: ExposureNotificationApi,
    private val exposureConfigurationApi: ExposureConfigurationApi,
    private val exposureWindowRiskCalculator: ExposureWindowRiskCalculator
) : ExposureRiskManager {
    override suspend fun getRisk(token: String): DayRisk? {
        val exposureConfiguration = exposureConfigurationApi.getExposureConfiguration()
        val riskCalculation = exposureConfiguration.v2RiskCalculation
        setDiagnosisKeysDataMappingIfNecessary(riskCalculation)

        val exposureWindows = exposureNotificationApi.getExposureWindows()

        val riskScoreCalculatorConfig = exposureConfiguration.riskScore.toRiskScoreCalculationConfiguration()
        return exposureWindowRiskCalculator(exposureWindows, riskCalculation, riskScoreCalculatorConfig)
    }

    private suspend fun setDiagnosisKeysDataMappingIfNecessary(riskCalculationConfiguration: V2RiskCalculation) {
        val dataMapping = riskCalculationConfiguration.dataMapping()
        if (exposureNotificationApi.getDiagnosisKeysDataMapping() != dataMapping) {
            try {
                exposureNotificationApi.setDiagnosisKeysDataMapping(dataMapping)
            } catch (exception: Exception) {
                Timber.e(exception)
            }
        }
    }

    private fun V2RiskCalculation.dataMapping(): DiagnosisKeysDataMapping =
        DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
            .setDaysSinceOnsetToInfectiousness(
                (-14..14).zip(this.daysSinceOnsetToInfectiousness).toMap()
            )
            .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.HIGH)
            .setReportTypeWhenMissing(reportTypeWhenMissing)
            .build()
}

private fun RiskScore.toRiskScoreCalculationConfiguration(): RiskScoreCalculatorConfiguration {
    return RiskScoreCalculatorConfiguration(
        sampleResolution = sampleResolution,
        expectedDistance = expectedDistance,
        minimumDistance = minimumDistance,
        rssiParameters = RssiParameters(
            weightCoefficient = rssiParameters.weightCoefficient,
            intercept = rssiParameters.intercept,
            covariance = rssiParameters.covariance
        ),
        powerLossParameters = PowerLossParameters(
            wavelength = powerLossParameters.wavelength,
            pathLossFactor = powerLossParameters.pathLossFactor,
            refDeviceLoss = powerLossParameters.refDeviceLoss
        ),
        observationType = observationType.toRiskScoreObservationType(),
        initialData = InitialData(
            mean = initialData.mean,
            covariance = initialData.covariance
        ),
        smootherParameters = SmootherParameters(
            alpha = smootherParameters.alpha,
            beta = smootherParameters.beta,
            kappa = smootherParameters.kappa
        )
    )
}

private fun ObservationType.toRiskScoreObservationType() = when (this) {
    log -> RiskScoreObservationType.log
    gen -> RiskScoreObservationType.gen
}
