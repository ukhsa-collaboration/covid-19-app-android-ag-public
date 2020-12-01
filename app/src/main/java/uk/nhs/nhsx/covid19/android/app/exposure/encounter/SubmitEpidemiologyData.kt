package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.analytics.MetadataProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.DayRisk
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyData
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness
import java.time.Instant
import javax.inject.Inject

class SubmitEpidemiologyData @Inject constructor(
    private val metadataProvider: MetadataProvider,
    private val epidemiologyDataApi: EpidemiologyDataApi
) {

    suspend operator fun invoke(
        exposureWindowWithRiskList: List<ExposureWindowWithRisk>,
        epidemiologyEventType: EpidemiologyEventType = EpidemiologyEventType.EXPOSURE_WINDOW
    ) {
        withContext(Dispatchers.IO) {
            exposureWindowWithRiskList.forEach { exposureWindowWithRisk ->
                epidemiologyDataApi.submitEpidemiologyData(
                    EpidemiologyData(
                        metadata = metadataProvider.getMetadata(),
                        events = listOf(createEpidemiologyEvent(exposureWindowWithRisk, epidemiologyEventType))
                    )
                )
            }
        }
    }

    private fun createEpidemiologyEvent(
        exposureWindowWithRisk: ExposureWindowWithRisk,
        epidemiologyEventType: EpidemiologyEventType
    ) =
        EpidemiologyEvent(
            type = epidemiologyEventType,
            version = 1,
            payload = EpidemiologyEventPayload(
                date = Instant.ofEpochMilli(exposureWindowWithRisk.dayRisk.startOfDayMillis),
                infectiousness = Infectiousness.fromInt(exposureWindowWithRisk.exposureWindow.infectiousness),
                scanInstances = exposureWindowWithRisk.exposureWindow.scanInstances.map { scanInstance ->
                    EpidemiologyEventPayloadScanInstance(
                        minimumAttenuation = scanInstance.minAttenuationDb,
                        secondsSinceLastScan = scanInstance.secondsSinceLastScan,
                        typicalAttenuation = scanInstance.typicalAttenuationDb
                    )
                },
                riskScore = exposureWindowWithRisk.dayRisk.calculatedRisk
            )
        )

    data class ExposureWindowWithRisk(val dayRisk: DayRisk, val exposureWindow: ExposureWindow)
}
