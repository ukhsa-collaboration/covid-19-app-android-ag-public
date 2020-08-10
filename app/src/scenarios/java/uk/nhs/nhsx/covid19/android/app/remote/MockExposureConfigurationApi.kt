package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureNotification
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation

class MockExposureConfigurationApi : ExposureConfigurationApi {

    override suspend fun getExposureConfiguration(): ExposureConfigurationResponse =
        ExposureConfigurationResponse(
            exposureNotification = ExposureNotification(
                minimumRiskScore = 11,
                attenuationDurationThresholds = listOf(55, 63),
                attenuationLevelValues = listOf(0, 1, 1, 1, 1, 1, 1, 1),
                daysSinceLastExposureLevelValues = listOf(5, 5, 5, 5, 5, 5, 5, 5),
                durationLevelValues = listOf(0, 0, 0, 1, 1, 1, 1, 0),
                transmissionRiskLevelValues = listOf(1, 3, 4, 5, 6, 7, 8, 6),
                attenuationWeight = 50.0,
                daysSinceLastExposureWeight = 20,
                durationWeight = 50.0,
                transmissionRiskWeight = 50.0
            ),
            riskCalculation = RiskCalculation(
                durationBucketWeights = listOf(1.0, 0.5, 0.0),
                riskThreshold = 900
            )
        )
}
