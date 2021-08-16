package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import android.content.SharedPreferences
import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider.Companion.EPIDEMIOLOGY_EVENT
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.HIGH
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class EpidemiologyEventProviderTest : ProviderTest<EpidemiologyEventProvider, List<EpidemiologyEvent>>() {

    private val fixedClock = Clock.fixed(Instant.parse("2020-12-18T14:38:40.180Z"), ZoneOffset.UTC)
    override val getTestSubject: (Moshi, SharedPreferences) -> EpidemiologyEventProvider =
        { moshi, sharedPreferences -> EpidemiologyEventProvider(fixedClock, moshi, sharedPreferences) }
    override val property = EpidemiologyEventProvider::epidemiologyEvents
    override val key = EPIDEMIOLOGY_EVENT
    override val defaultValue: List<EpidemiologyEvent> = emptyList()
    override val expectations: List<ProviderTestExpectation<List<EpidemiologyEvent>>> = listOf(
        ProviderTestExpectation(json = oldEpidemiologyEvent1Json, objectValue = listOf(epidemiologyEvent1), direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = epidemiologyEvent1Json, objectValue = listOf(epidemiologyEvent1), direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = epidemiologyEvent2Json, objectValue = listOf(epidemiologyEvent2), direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = epidemiologyEvents1And3And4Json, objectValue = listOf(epidemiologyEvent1, epidemiologyEvent3, epidemiologyEvent4), direction = JSON_TO_OBJECT)
    )

    @Test
    fun `test adding single risky epidemiology event`() {
        sharedPreferencesReturns(epidemiologyEvent1Json)

        testSubject.addRiskyEpidemiologyEvents(listOf(epidemiologyEvent2))

        assertSharedPreferenceSetsValue(epidemiologyEvents1And2Json)
    }

    @Test
    fun `test adding single non risky epidemiology event with storage limit 2`() {
        sharedPreferencesReturns(epidemiologyEvents1And2Json)

        testSubject.addNonRiskyEpidemiologyEvents(
            listOf(epidemiologyEvent3),
            storageLimit = 2
        )

        assertSharedPreferenceSetsValue(epidemiologyEvents1And2And3Json)
    }

    @Test
    fun `test adding two non risky epidemiology events with storage limit 2`() {
        sharedPreferencesReturns(epidemiologyEvents1And2Json)

        testSubject.addNonRiskyEpidemiologyEvents(
            listOf(epidemiologyEvent3, epidemiologyEvent4),
            storageLimit = 2
        )

        assertSharedPreferenceSetsValue(epidemiologyEvents1And3And4Json)
    }

    @Test
    fun `test adding three non risky epidemiology events with storage limit 2`() {
        sharedPreferencesReturns(epidemiologyEvents1And2Json)

        testSubject.addNonRiskyEpidemiologyEvents(
            listOf(epidemiologyEvent3, epidemiologyEvent4, epidemiologyEvent5),
            storageLimit = 2
        )

        assertSharedPreferenceSetsValue(epidemiologyEvents1And4And5Json)
    }

    @Test
    fun `test clearing epidemiology events with date of oldest event keeps newer one`() {
        sharedPreferencesReturns(epidemiologyEvents1And2Json)

        testSubject.clearOnAndBefore(event1LocalDate)

        assertSharedPreferenceSetsValue(epidemiologyEvent2Json)
    }

    @Test
    fun `test clearing epidemiology events with date of oldest event + 1 day removes it`() {
        sharedPreferencesReturns(epidemiologyEvents1And2Json)

        testSubject.clearOnAndBefore(event1LocalDate.plusDays(1))

        assertSharedPreferenceSetsValue(epidemiologyEvent2Json)
    }

    @Test
    fun `test clearing epidemiology events with date of newest event - 1 keeps it`() {
        sharedPreferencesReturns(epidemiologyEvents1And2Json)

        testSubject.clearOnAndBefore(event2LocalDate.minusDays(1))

        assertSharedPreferenceSetsValue(epidemiologyEvent2Json)
    }

    @Test
    fun `can migrate`() {
        sharedPreferencesReturns(oldEpidemiologyEvent1Json)

        testSubject.addRiskyEpidemiologyEvents(listOf(epidemiologyEvent2))

        assertSharedPreferenceSetsValue(epidemiologyEvents1And2Json)
    }

    @Test
    fun `test clearing epidemiology events with date of newest event removes all`() {
        sharedPreferencesReturns(epidemiologyEvents1And2Json)

        testSubject.clearOnAndBefore(event2LocalDate)

        assertSharedPreferenceSetsValue("[]")
    }

    @Test
    fun `test clearing epidemiology events with date of newest event + 1 day removes all`() {
        sharedPreferencesReturns(epidemiologyEvents1And2Json)

        testSubject.clearOnAndBefore(event2LocalDate.plusDays(1))

        assertSharedPreferenceSetsValue("[]")
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
            isConsideredRisky: Boolean = false,
            date: String = event2Date,
            riskCalculationVersion: Int = 2,
            scanInstances: List<EpidemiologyEventPayloadScanInstance> = emptyList()
        ) =
            EpidemiologyEvent(
                payload = EpidemiologyEventPayload(
                    date = Instant.parse(date),
                    infectiousness = HIGH,
                    scanInstances = scanInstances,
                    riskScore = riskScore,
                    riskCalculationVersion = riskCalculationVersion,
                    isConsideredRisky = isConsideredRisky
                )
            )

        private val epidemiologyEvent1 = createNonRiskyEpidemiologyEvent(10.0, true, event1Date, 1)

        private val epidemiologyEvent2 = createNonRiskyEpidemiologyEvent(
            riskScore = 2.0,
            scanInstances = listOf(
                EpidemiologyEventPayloadScanInstance(1, 0, 1)
            )
        )

        private val epidemiologyEvent3 = createNonRiskyEpidemiologyEvent(3.0)

        private val epidemiologyEvent4 = createNonRiskyEpidemiologyEvent(4.0)

        private val epidemiologyEvent5 = createNonRiskyEpidemiologyEvent(5.0)
    }
}
