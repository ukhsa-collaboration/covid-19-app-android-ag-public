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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class EpidemiologyEventProviderTest {

    private val storage = mockk<EpidemiologyEventStorage>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-12-18T14:38:40.180Z"), ZoneOffset.UTC)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    val testSubject = EpidemiologyEventProvider(storage, fixedClock, moshi)

    @Test
    fun `test adding single risky epidemiology event`() {
        every { storage.value } returns epidemiologyEvent1Json

        testSubject.addRiskyEpidemiologyEvents(listOf(epidemiologyEvent2))

        verify { storage.value = epidemiologyEvents1And2Json }
    }

    @Test
    fun `test adding single non risky epidemiology event with storage limit 2`() {
        every { storage.value } returns epidemiologyEvents1And2Json

        testSubject.addNonRiskyEpidemiologyEvents(
            listOf(epidemiologyEvent3),
            storageLimit = 2
        )

        verify { storage.value = epidemiologyEvents1And2And3Json }
    }

    @Test
    fun `test adding two non risky epidemiology events with storage limit 2`() {
        every { storage.value } returns epidemiologyEvents1And2Json

        testSubject.addNonRiskyEpidemiologyEvents(
            listOf(epidemiologyEvent3, epidemiologyEvent4),
            storageLimit = 2
        )

        verify { storage.value = epidemiologyEvents1And3And4Json }
    }

    @Test
    fun `test adding three non risky epidemiology events with storage limit 2`() {
        every { storage.value } returns epidemiologyEvents1And2Json

        testSubject.addNonRiskyEpidemiologyEvents(
            listOf(epidemiologyEvent3, epidemiologyEvent4, epidemiologyEvent5),
            storageLimit = 2
        )

        verify { storage.value = epidemiologyEvents1And4And5Json }
    }

    @Test
    fun `test clearing epidemiology events with date of oldest event keeps newer one`() {
        every { storage.value } returns epidemiologyEvents1And2Json

        testSubject.clearOnAndBefore(event1LocalDate)

        verify { storage.value = epidemiologyEvent2Json }
    }

    @Test
    fun `test clearing epidemiology events with date of oldest event + 1 day removes it`() {
        every { storage.value } returns epidemiologyEvents1And2Json

        testSubject.clearOnAndBefore(event1LocalDate.plusDays(1))

        verify { storage.value = epidemiologyEvent2Json }
    }

    @Test
    fun `test clearing epidemiology events with date of newest event - 1 keeps it`() {
        every { storage.value } returns epidemiologyEvents1And2Json

        testSubject.clearOnAndBefore(event2LocalDate.minusDays(1))

        verify { storage.value = epidemiologyEvent2Json }
    }

    @Test
    fun `can migrate`() {
        every { storage.value } returns oldEpidemiologyEvent1Json

        testSubject.addRiskyEpidemiologyEvents(listOf(epidemiologyEvent2))

        verify { storage.value = epidemiologyEvents1And2Json }
    }

    @Test
    fun `test clearing epidemiology events with date of newest event removes all`() {
        every { storage.value } returns epidemiologyEvents1And2Json

        testSubject.clearOnAndBefore(event2LocalDate)

        verify { storage.value = "[]" }
    }

    @Test
    fun `test clearing epidemiology events with date of newest event + 1 day removes all`() {
        every { storage.value } returns epidemiologyEvents1And2Json

        testSubject.clearOnAndBefore(event2LocalDate.plusDays(1))

        verify { storage.value = "[]" }
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
                matchedKeyCount = 1,
                isConsideredRisky = false
            )

        val expectedEvent = EpidemiologyEvent(
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
                riskCalculationVersion = 2,
                isConsideredRisky = false
            )
        )

        assertEquals(expectedEvent, exposureWindowWithRisk.toEpidemiologyEvent())
    }

    companion object {
        private const val event1Date =
            """2020-12-15T14:38:40.180Z"""
        private val event1LocalDate = LocalDate.of(2020, 12, 15)
        private const val event2Date =
            """2020-12-18T14:38:40.180Z"""
        private val event2LocalDate = LocalDate.of(2020, 12, 18)

        private val oldEpidemiologyEvent1Json =
            """
            [{"version":1,"payload":{"date":"$event1Date","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}}]
            """.trim()

        private val epidemiologyEvent1Json =
            """
            [{"payload":{"isConsideredRisky":true,"date":"$event1Date","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}}]
            """.trim()

        private val epidemiologyEvent2Json =
            """
            [{"payload":{"isConsideredRisky":false,"date":"$event2Date","infectiousness":"high","scanInstances":[{"minimumAttenuation":1,"secondsSinceLastScan":0,"typicalAttenuation":1}],"riskScore":2.0,"riskCalculationVersion":2}}]
            """.trim()

        private val epidemiologyEvents1And2Json =
            """
            [{"payload":{"isConsideredRisky":true,"date":"$event1Date","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}},{"payload":{"isConsideredRisky":false,"date":"$event2Date","infectiousness":"high","scanInstances":[{"minimumAttenuation":1,"secondsSinceLastScan":0,"typicalAttenuation":1}],"riskScore":2.0,"riskCalculationVersion":2}}]
            """.trim()

        private val epidemiologyEvents1And2And3Json =
            """
            [{"payload":{"isConsideredRisky":true,"date":"$event1Date","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}},{"payload":{"isConsideredRisky":false,"date":"$event2Date","infectiousness":"high","scanInstances":[{"minimumAttenuation":1,"secondsSinceLastScan":0,"typicalAttenuation":1}],"riskScore":2.0,"riskCalculationVersion":2}},{"payload":{"isConsideredRisky":false,"date":"$event2Date","infectiousness":"high","scanInstances":[],"riskScore":3.0,"riskCalculationVersion":2}}]
            """.trim()

        private val epidemiologyEvents1And3And4Json =
            """
            [{"payload":{"isConsideredRisky":true,"date":"$event1Date","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}},{"payload":{"isConsideredRisky":false,"date":"$event2Date","infectiousness":"high","scanInstances":[],"riskScore":3.0,"riskCalculationVersion":2}},{"payload":{"isConsideredRisky":false,"date":"$event2Date","infectiousness":"high","scanInstances":[],"riskScore":4.0,"riskCalculationVersion":2}}]
            """.trim()

        private val epidemiologyEvents1And4And5Json =
            """
            [{"payload":{"isConsideredRisky":true,"date":"$event1Date","infectiousness":"high","scanInstances":[],"riskScore":10.0,"riskCalculationVersion":1}},{"payload":{"isConsideredRisky":false,"date":"$event2Date","infectiousness":"high","scanInstances":[],"riskScore":4.0,"riskCalculationVersion":2}},{"payload":{"isConsideredRisky":false,"date":"$event2Date","infectiousness":"high","scanInstances":[],"riskScore":5.0,"riskCalculationVersion":2}}]
            """.trim()

        private fun createNonRiskyEpidemiologyEvent(
            riskScore: Double,
            scanInstances: List<EpidemiologyEventPayloadScanInstance>
        ) =
            EpidemiologyEvent(
                payload = EpidemiologyEventPayload(
                    date = Instant.parse(event2Date),
                    infectiousness = HIGH,
                    scanInstances = scanInstances,
                    riskScore = riskScore,
                    riskCalculationVersion = 2,
                    isConsideredRisky = false
                )
            )

        private val epidemiologyEvent2 = createNonRiskyEpidemiologyEvent(
            2.0,
            listOf(
                EpidemiologyEventPayloadScanInstance(1, 0, 1)
            )
        )

        private val epidemiologyEvent3 = createNonRiskyEpidemiologyEvent(3.0, emptyList())

        private val epidemiologyEvent4 = createNonRiskyEpidemiologyEvent(4.0, emptyList())

        private val epidemiologyEvent5 = createNonRiskyEpidemiologyEvent(5.0, emptyList())
    }
}
