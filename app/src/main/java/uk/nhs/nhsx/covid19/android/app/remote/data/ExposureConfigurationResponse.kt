package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExposureConfigurationResponse(
    val exposureNotification: ExposureNotification,
    val v2RiskCalculation: V2RiskCalculation,
    val riskScore: RiskScore
)

@JsonClass(generateAdapter = true)
data class ExposureNotification(
    val attenuationDurationThresholds: List<Int>,
    val attenuationLevelValues: List<Int>,
    val attenuationWeight: Double,
    val daysSinceLastExposureLevelValues: List<Int>,
    val daysSinceLastExposureWeight: Int,
    val durationLevelValues: List<Int>,
    val durationWeight: Double,
    val minimumRiskScore: Int,
    val transmissionRiskLevelValues: List<Int>,
    val transmissionRiskWeight: Double
)

@JsonClass(generateAdapter = true)
data class V2RiskCalculation(
    val daysSinceOnsetToInfectiousness: List<Int>,
    val infectiousnessWeights: List<Double>,
    val reportTypeWhenMissing: Int,
    val riskThreshold: Double
)

@JsonClass(generateAdapter = true)
data class RiskScore(
    val sampleResolution: Double,
    val expectedDistance: Double,
    val minimumDistance: Double,
    val rssiParameters: RssiParameters,
    val powerLossParameters: PowerLossParameters,
    val observationType: ObservationType,
    val initialData: InitialData,
    val smootherParameters: SmootherParameters
) {
    @JsonClass(generateAdapter = true)
    data class RssiParameters(
        val weightCoefficient: Double,
        val intercept: Double,
        val covariance: Double
    )

    @JsonClass(generateAdapter = true)
    data class PowerLossParameters(
        val wavelength: Double,
        val pathLossFactor: Double,
        val refDeviceLoss: Double
    )

    enum class ObservationType {
        log,
        gen
    }

    @JsonClass(generateAdapter = true)
    data class InitialData(
        val mean: Double,
        val covariance: Double
    )

    @JsonClass(generateAdapter = true)
    data class SmootherParameters(
        val alpha: Double,
        val beta: Double,
        val kappa: Double
    )
}
