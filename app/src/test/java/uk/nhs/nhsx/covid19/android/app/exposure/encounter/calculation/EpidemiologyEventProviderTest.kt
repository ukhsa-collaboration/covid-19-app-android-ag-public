package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.HIGH
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Instant
import kotlin.test.assertEquals

class EpidemiologyEventProviderTest {

    private val storage = mockk<EpidemiologyEventStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    @Test
    fun `can add single epidemiology event`() {
        val event = EpidemiologyEvent(
            version = 1,
            payload = EpidemiologyEventPayload(
                date = Instant.parse("2020-11-18T14:38:40.180Z"),
                infectiousness = HIGH,
                scanInstances = listOf(
                    EpidemiologyEventPayloadScanInstance(1, 0, 1)
                ),
                riskScore = 10.0,
                riskCalculationVersion = 2
            )
        )
        every { storage.value } returns SINGLE_EPIDEMIOLOGY_EVENT_JSON

        val testSubject = EpidemiologyEventProvider(storage, moshi)
        testSubject.add(listOf(event))

        verify { storage.value = MULTIPLE_EXPOSURE_WINDOW_JSON }
    }

    @Test
    fun `can clear epidemiology events`() {
        every { storage.value } returns SINGLE_EPIDEMIOLOGY_EVENT_JSON
        val testSubject = EpidemiologyEventProvider(storage, moshi)
        testSubject.clear()

        verify { storage.value = null }
    }

    @Test
    fun `can convert exposure window to epidemiology event`() {
        val scanInstance =
            ScanInstance.Builder()
                .setMinAttenuationDb(1)
                .setSecondsSinceLastScan(2)
                .setTypicalAttenuationDb(3)
                .build()

        val date = Instant.parse("2020-11-18T13:20:36.875Z")

        val exposureWindow =
            ExposureWindow.Builder()
                .setDateMillisSinceEpoch(date.toEpochMilli())
                .setInfectiousness(Infectiousness.HIGH)
                .setScanInstances(listOf(scanInstance))
                .build()

        val exposureWindowWithRisk =
            ExposureWindowWithRisk(
                exposureWindow,
                calculatedRisk = 10.0,
                riskCalculationVersion = 2,
                matchedKeyCount = 1
            )

        val expectedEvent = EpidemiologyEvent(
            version = 1,
            payload = EpidemiologyEventPayload(
                date = date,
                infectiousness = uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.fromInt(2),
                scanInstances = listOf(
                    EpidemiologyEventPayloadScanInstance(
                        minimumAttenuation = 1,
                        secondsSinceLastScan = 2,
                        typicalAttenuation = 3
                    )
                ),
                riskScore = 10.0,
                riskCalculationVersion = 2
            )
        )

        assertEquals(expectedEvent, exposureWindowWithRisk.toEpidemiologyEvent())
    }

    companion object {
        private val SINGLE_EPIDEMIOLOGY_EVENT_JSON =
            """
            [{"version":1,"payload":{"date":"2020-11-08T14:38:40.180Z","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}}]
            """.trim()

        private val MULTIPLE_EXPOSURE_WINDOW_JSON =
            """
            [{"version":1,"payload":{"date":"2020-11-08T14:38:40.180Z","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}},{"version":1,"payload":{"date":"2020-11-18T14:38:40.180Z","infectiousness":"high","scanInstances":[{"minimumAttenuation":1,"secondsSinceLastScan":0,"typicalAttenuation":1}],"riskScore":10.0,"riskCalculationVersion":2}}]
            """.trim()
    }
}
