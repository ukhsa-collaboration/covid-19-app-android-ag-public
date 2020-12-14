package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Instant

class EpidemiologyEventProviderTest {

    private val storage = mockk<EpidemiologyEventStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    @Test
    fun `can add single epidemiology event`() {
        val event = EpidemiologyEvent(
            EpidemiologyEventType.EXPOSURE_WINDOW, 1,
            EpidemiologyEventPayload(
                Instant.parse("2020-11-18T14:38:40.180Z"), Infectiousness.fromInt(2),
                listOf(
                    EpidemiologyEventPayloadScanInstance(1, 0, 1)
                ),
                10.0,
                2
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

    companion object {
        private val SINGLE_EPIDEMIOLOGY_EVENT_JSON =
            """
            [{"type":"exposureWindow","version":1,"payload":{"date":"2020-11-08T14:38:40.180Z","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}}]
            """.trim()

        private val MULTIPLE_EXPOSURE_WINDOW_JSON =
            """
            [{"type":"exposureWindow","version":1,"payload":{"date":"2020-11-08T14:38:40.180Z","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}},{"type":"exposureWindow","version":1,"payload":{"date":"2020-11-18T14:38:40.180Z","infectiousness":"high","scanInstances":[{"minimumAttenuation":1,"secondsSinceLastScan":0,"typicalAttenuation":1}],"riskScore":10.0,"riskCalculationVersion":2}}]
            """.trim()
    }
}
