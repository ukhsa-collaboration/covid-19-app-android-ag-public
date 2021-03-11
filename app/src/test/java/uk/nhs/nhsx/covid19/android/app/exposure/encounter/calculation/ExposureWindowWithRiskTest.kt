package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExposureWindowWithRiskTest {

    @Test
    fun `convert ExposureWindowWithRisk to EpidemiologyEvent`() {
        val expected = EpidemiologyEvent(
            version = 1,
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
                riskCalculationVersion = exposureWindowWithRisk.riskCalculationVersion
            )
        )

        val actual = exposureWindowWithRisk.toEpidemiologyEvent()

        assertEquals(expected, actual)
    }

    @Test
    fun `is above threshold is true when calculated risk is larger than threshold`() {
        assertTrue(exposureWindowWithRisk.copy(calculatedRisk = 120.0).isAboveThreshold(100.0))
    }

    @Test
    fun `is above threshold is true when calculated risk is equal to threshold`() {
        assertTrue(exposureWindowWithRisk.copy(calculatedRisk = 100.0).isAboveThreshold(100.0))
    }

    @Test
    fun `is above threshold is false when calculated risk is less than threshold`() {
        assertFalse(exposureWindowWithRisk.copy(calculatedRisk = 90.0).isAboveThreshold(100.0))
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
        matchedKeyCount = 0
    )
}
