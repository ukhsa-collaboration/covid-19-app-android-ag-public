package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import javax.inject.Inject

class ExposureRiskManagerProvider @Inject constructor(
    private val exposureNotificationApi: ExposureNotificationApi,
    private val exposureInformationRiskManager: ExposureInformationRiskManager,
    private val exposureWindowRiskManager: ExposureWindowRiskManager
) {
    suspend fun riskManager(): ExposureRiskManager =
        exposureNotificationApi.version()
            ?.let { exposureWindowRiskManager }
            ?: exposureInformationRiskManager
}
