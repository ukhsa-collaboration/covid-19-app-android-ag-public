package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

data class ExposureWindowWithRisk(
    val exposureWindow: ExposureWindow,
    val calculatedRisk: Double,
    val riskCalculationVersion: Int,
    val matchedKeyCount: Int
) {
    val startOfDayMillis: Long = exposureWindow.dateMillisSinceEpoch
    val encounterDate: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startOfDayMillis), ZoneOffset.UTC)

    fun isAboveThreshold(riskThreshold: Double) = calculatedRisk >= riskThreshold

    fun toEpidemiologyEvent(): EpidemiologyEvent =
        EpidemiologyEvent(
            version = 1,
            payload = EpidemiologyEventPayload(
                date = Instant.ofEpochMilli(startOfDayMillis),
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
