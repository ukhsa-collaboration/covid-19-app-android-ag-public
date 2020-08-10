package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExposureConfigurationResponse(
    val exposureNotification: ExposureNotification,
    val riskCalculation: RiskCalculation
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
data class RiskCalculation(
    val durationBucketWeights: List<Double>,
    val riskThreshold: Int
)
