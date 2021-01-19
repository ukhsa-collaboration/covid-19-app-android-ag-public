package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import java.time.Instant
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness

data class DayRisk(
    val startOfDayMillis: Long,
    val calculatedRisk: Double,
    val riskCalculationVersion: Int,
    val matchedKeyCount: Int,
    val exposureWindows: List<ExposureWindow>
)

fun DayRisk.toEpidemiologyEvents(): List<EpidemiologyEvent> =
    exposureWindows.map {
        EpidemiologyEvent(
            version = 1,
            payload = EpidemiologyEventPayload(
                date = Instant.ofEpochMilli(startOfDayMillis),
                infectiousness = Infectiousness.fromInt(it.infectiousness),
                scanInstances = it.scanInstances.map { scanInstance ->
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
