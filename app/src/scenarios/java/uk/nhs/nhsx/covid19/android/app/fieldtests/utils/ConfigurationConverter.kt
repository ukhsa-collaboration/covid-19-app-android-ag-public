package uk.nhs.nhsx.covid19.android.app.fieldtests.utils

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import uk.nhs.nhsx.covid19.android.app.exposure.FieldTestConfiguration

object ConfigurationConverter {
    fun toExposureConfiguration(configuration: FieldTestConfiguration): ExposureConfiguration {
        return configuration.run {
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
        }
    }
}
