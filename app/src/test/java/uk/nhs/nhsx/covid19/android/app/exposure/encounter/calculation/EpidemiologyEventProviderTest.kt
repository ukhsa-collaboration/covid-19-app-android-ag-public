package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData
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
        val dayRisk =
            DayRisk(
                startOfDayMillis = Instant.parse("2020-11-18T13:20:36.875Z").toEpochMilli(),
                calculatedRisk = 10.0,
                riskCalculationVersion = 2
            )
        val scanInstance =
            ScanInstance.Builder()
                .setSecondsSinceLastScan(0)
                .setMinAttenuationDb(1)
                .setTypicalAttenuationDb(1)
                .build()

        val exposureWindow =
            ExposureWindow.Builder()
                .setInfectiousness(Infectiousness.HIGH)
                .setScanInstances(listOf(scanInstance))
                .build()

        val exposureWindowWithRisk = SubmitEpidemiologyData.ExposureWindowWithRisk(dayRisk, exposureWindow)

        val event = EpidemiologyEvent(
            version = 1,
            payload = EpidemiologyEventPayload(
                Instant.parse("2020-11-18T13:20:36.875Z"),
                uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.fromInt(2),
                listOf(EpidemiologyEventPayloadScanInstance(1, 0, 1)),
                10.0,
                2
            )
        )

        assertEquals(event, exposureWindowWithRisk.toEpidemiologyEvent())
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
