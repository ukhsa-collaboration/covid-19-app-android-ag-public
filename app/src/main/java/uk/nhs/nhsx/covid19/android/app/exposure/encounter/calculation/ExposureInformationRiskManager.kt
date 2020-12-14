package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.ExposureConfigurationApi
import javax.inject.Inject

class ExposureInformationRiskManager(
    private val exposureNotificationApi: ExposureNotificationApi,
    private val configurationApi: ExposureConfigurationApi,
    private val riskCalculator: RiskCalculator
) : ExposureRiskManager {

    @Inject
    constructor(
        exposureNotificationApi: ExposureNotificationApi,
        configurationApi: ExposureConfigurationApi
    ) : this(
        exposureNotificationApi,
        configurationApi,
        RiskCalculator()
    )

    override suspend fun getRisk(token: String): DayRisk? {
        val exposureInformationList = exposureNotificationApi.getExposureInformation(token)
        val exposureConfiguration = configurationApi.getExposureConfiguration()

        return riskCalculator(
            exposureInformationList,
            exposureConfiguration.riskCalculation
        ).also { calculatedRisk ->
            val threshold = exposureConfiguration.riskCalculation.riskThreshold
            Timber.d("calculatedRisk: $calculatedRisk threshold: $threshold")
        }
    }
}
