package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FieldTestConfiguration(
    val minimumRiskScore: Int = 4,
    val attenuationLevelValues: List<Int> = listOf(4, 4, 4, 4, 4, 4, 4, 4),
    val attenuationWeight: Double = 50.0,
    val daysSinceLastExposureLevelValues: List<Int> = listOf(4, 4, 4, 4, 4, 4, 4, 4),
    val daysSinceLastExposureWeight: Double = 50.0,
    val durationLevelValues: List<Int> = listOf(4, 4, 4, 4, 4, 4, 4, 4),
    val durationWeight: Double = 50.0,
    val transmissionRiskLevelValues: List<Int> = listOf(4, 4, 4, 4, 4, 4, 4, 4),
    val transmissionRiskWeight: Double = 50.0,
    val attenuationDurationThresholds: List<Int> = listOf(50, 70),
    val durationBucketWeights: List<Double> = listOf(1.0, 1.0, 1.0)
)

fun FieldTestConfiguration.toExposureConfiguration(): ExposureConfiguration =
    ExposureConfiguration.ExposureConfigurationBuilder()
        .setMinimumRiskScore(minimumRiskScore)
        .setAttenuationScores(*attenuationLevelValues.toIntArray())
        .setAttenuationWeight(attenuationWeight.toInt())
        .setDaysSinceLastExposureScores(*daysSinceLastExposureLevelValues.toIntArray())
        .setDaysSinceLastExposureWeight(daysSinceLastExposureWeight.toInt())
        .setDurationScores(*durationLevelValues.toIntArray())
        .setDurationWeight(durationWeight.toInt())
        .setTransmissionRiskScores(*transmissionRiskLevelValues.toIntArray())
        .setTransmissionRiskWeight(transmissionRiskWeight.toInt())
        .setDurationAtAttenuationThresholds(*attenuationDurationThresholds.toIntArray())
        .build()
