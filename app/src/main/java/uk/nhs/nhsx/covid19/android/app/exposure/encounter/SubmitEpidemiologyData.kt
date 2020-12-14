package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.analytics.MetadataProvider
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.DayRisk
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyData
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import java.time.Instant
import javax.inject.Inject

class SubmitEpidemiologyData @Inject constructor(
    private val metadataProvider: MetadataProvider,
    private val epidemiologyDataApi: EpidemiologyDataApi,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows
) {

    suspend operator fun invoke(
        exposureWindowWithRiskList: List<ExposureWindowWithRisk>,
        epidemiologyEventType: EpidemiologyEventType = EpidemiologyEventType.EXPOSURE_WINDOW
    ) {
        withContext(Dispatchers.IO) {
            exposureWindowWithRiskList.forEach { exposureWindowWithRisk ->
                runSafely {
                    epidemiologyDataApi.submitEpidemiologyData(
                        EpidemiologyData(
                            metadata = metadataProvider.getMetadata(),
                            events = listOf(exposureWindowWithRisk.convert(epidemiologyEventType))
                        )
                    )
                }
            }
            submitFakeExposureWindows(EXPOSURE_WINDOW, exposureWindowWithRiskList.size)
        }
    }

    data class ExposureWindowWithRisk(val dayRisk: DayRisk, val exposureWindow: ExposureWindow)
}

fun SubmitEpidemiologyData.ExposureWindowWithRisk.convert(
    epidemiologyEventType: EpidemiologyEventType
): EpidemiologyEvent {
    return EpidemiologyEvent(
        type = epidemiologyEventType,
        version = 1,
        payload = EpidemiologyEventPayload(
            date = Instant.ofEpochMilli(this.dayRisk.startOfDayMillis),
            infectiousness = Infectiousness.fromInt(this.exposureWindow.infectiousness),
            scanInstances = this.exposureWindow.scanInstances.map { scanInstance ->
                EpidemiologyEventPayloadScanInstance(
                    minimumAttenuation = scanInstance.minAttenuationDb,
                    secondsSinceLastScan = scanInstance.secondsSinceLastScan,
                    typicalAttenuation = scanInstance.typicalAttenuationDb
                )
            },
            riskScore = this.dayRisk.calculatedRisk,
            riskCalculationVersion = this.dayRisk.riskCalculationVersion
        )
    )
}
