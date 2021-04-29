package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import java.time.Instant
import kotlin.test.assertEquals

class ExposureWindowWithRiskTest {

    @Test
    fun `convert ExposureWindowWithRisk to EpidemiologyEvent`() {
        val expected = EpidemiologyEvent(
            payload = EpidemiologyEventPayload(
                testType = null,
                requiresConfirmatoryTest = null,
                date = Instant.ofEpochMilli(exposureWindowWithRisk.startOfDayMillis),
                infectiousness = uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.fromInt(
                    exposureWindowWithRisk.exposureWindow.infectiousness
                ),
                scanInstances = exposureWindowWithRisk.exposureWindow.scanInstances.map { scanInstance ->
                    EpidemiologyEventPayloadScanInstance(
                        minimumAttenuation = scanInstance.minAttenuationDb,
                        secondsSinceLastScan = scanInstance.secondsSinceLastScan,
                        typicalAttenuation = scanInstance.typicalAttenuationDb
                    )
                },
                riskScore = exposureWindowWithRisk.calculatedRisk,
                riskCalculationVersion = exposureWindowWithRisk.riskCalculationVersion,
                isConsideredRisky = true
            )
        )

        val actual = exposureWindowWithRisk.toEpidemiologyEvent()

        assertEquals(expected, actual)
    }

    private val exposureWindowFromApi = ExposureWindow.Builder()
        .setDateMillisSinceEpoch(123L)
        .setInfectiousness(Infectiousness.HIGH)
        .setScanInstances(
            listOf(
                ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build(),
                ScanInstance.Builder().setMinAttenuationDb(40).setSecondsSinceLastScan(240).build()
            )
        )
        .build()

    private val exposureWindowWithRisk = ExposureWindowWithRisk(
        exposureWindowFromApi,
        calculatedRisk = 120.0,
        riskCalculationVersion = 0,
        matchedKeyCount = 0,
        isConsideredRisky = true
    )
}
