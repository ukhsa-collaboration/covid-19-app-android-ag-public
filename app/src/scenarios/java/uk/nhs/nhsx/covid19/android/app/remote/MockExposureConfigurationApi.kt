package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.ExposureNotification
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskCalculation
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.InitialData
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.ObservationType.log
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.PowerLossParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.RssiParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskScore.SmootherParameters
import uk.nhs.nhsx.covid19.android.app.remote.data.V2RiskCalculation

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
            ),
            v2RiskCalculation = V2RiskCalculation(
                daysSinceOnsetToInfectiousness = listOf(
                    0, 0, 0, 0, 0, 0, 0, 0, 0,
                    1, 1, 1,
                    2, 2, 2, 2, 2, 2,
                    1, 1, 1, 1, 1, 1,
                    0, 0, 0, 0, 0
                ),
                infectiousnessWeights = listOf(0.0, 0.4, 1.0),
                reportTypeWhenMissing = 1,
                riskThreshold = 1.0
            ),
            riskScore = RiskScore(
                sampleResolution = 1.0,
                expectedDistance = 0.1,
                minimumDistance = 1.0,
                rssiParameters = RssiParameters(
                    weightCoefficient = 0.1270547531082051,
                    intercept = 4.2309333657856945,
                    covariance = 0.4947614361027773
                ),
                powerLossParameters = PowerLossParameters(
                    wavelength = 0.125,
                    pathLossFactor = 20.0,
                    refDeviceLoss = 0.0
                ),
                observationType = log,
                initialData = InitialData(
                    mean = 2.0,
                    covariance = 10.0
                ),
                smootherParameters = SmootherParameters(
                    alpha = 1.0,
                    beta = 0.0,
                    kappa = 0.0
                )
            )
        )
}
