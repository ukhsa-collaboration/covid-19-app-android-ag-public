package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness
import java.time.Instant

data class ExposureWindowWithRisk(
    val exposureWindow: ExposureWindow,
    val calculatedRisk: Double,
    val isConsideredRisky: Boolean,
    val riskCalculationVersion: Int,
    val matchedKeyCount: Int
) {
    val startOfDayMillis: Long = exposureWindow.dateMillisSinceEpoch

    fun toEpidemiologyEvent(): EpidemiologyEvent =
        EpidemiologyEvent(
            payload = EpidemiologyEventPayload(
                date = Instant.ofEpochMilli(startOfDayMillis),
                isConsideredRisky = isConsideredRisky,
                infectiousness = Infectiousness.fromInt(exposureWindow.infectiousness),
                scanInstances = exposureWindow.scanInstances.map { scanInstance ->
                    EpidemiologyEventPayloadScanInstance(
                        minimumAttenuation = scanInstance.minAttenuationDb,
                        secondsSinceLastScan = scanInstance.secondsSinceLastScan,
                        typicalAttenuation = scanInstance.typicalAttenuationDb
                    )
                },
                riskScore = calculatedRisk,
                riskCalculationVersion = riskCalculationVersion
            )
        )
}
