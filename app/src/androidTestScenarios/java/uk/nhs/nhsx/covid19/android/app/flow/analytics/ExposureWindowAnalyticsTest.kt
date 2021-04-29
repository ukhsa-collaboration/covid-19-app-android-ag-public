package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.google.android.gms.nearby.exposurenotification.ScanInstance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.SymptomsAndOnsetFlowConfiguration
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW_POSITIVE_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventWithType
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import kotlin.test.assertEquals

class ExposureWindowAnalyticsTest : AnalyticsTest() {
    private val riskyContact = RiskyContact(this)
    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)

    private val events: List<EpidemiologyEventWithType>
        get() = testAppContext.epidemiologyDataApi.requests.flatMap {
            it.events
        }

    @Test
    fun submitsExposureWindowData_whenUserHasRiskyContact() = runBlocking {
        testAppContext.epidemiologyDataApi.clear()

        riskyContact.triggerViaBroadcastReceiver()

        val expectedEvents = getEncounterEpidemiologyEvents()

        assertEquals(expectedEvents, events)
    }

    @Test
    fun submitsExposureWindowData_whenUserReceivesPositiveTest() = runBlocking {
        riskyContact.triggerViaBroadcastReceiver()

        testAppContext.epidemiologyDataApi.clear()

        manualTestResultEntry.enterPositive(
            LAB_RESULT,
            SymptomsAndOnsetFlowConfiguration(),
            expectedScreenState = PositiveContinueIsolation
        )

        val expectedEvents = getPositiveTestEpidemiologyEvents()

        assertEquals(expectedEvents, events)
    }

    private suspend fun getEncounterEpidemiologyEvents(): List<EpidemiologyEventWithType> {
        val exposureWindows = testAppContext.getExposureNotificationApi().getExposureWindows()
        return listOf(
            getEpidemiologyEvent(
                EXPOSURE_WINDOW,
                version = 2,
                riskScore = 312.8639553203358,
                isConsideredRisky = true,
                scanInstances = exposureWindows[0].scanInstances
            ),
            getEpidemiologyEvent(
                EXPOSURE_WINDOW,
                version = 2,
                riskScore = 0.4272038640881409,
                isConsideredRisky = false,
                scanInstances = exposureWindows[1].scanInstances
            )
        )
    }

    private suspend fun getPositiveTestEpidemiologyEvents(): List<EpidemiologyEventWithType> {
        val exposureWindows = testAppContext.getExposureNotificationApi().getExposureWindows()
        return listOf(
            getEpidemiologyEvent(
                EXPOSURE_WINDOW_POSITIVE_TEST,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false,
                version = 3,
                riskScore = 312.8639553203358,
                isConsideredRisky = true,
                scanInstances = exposureWindows[0].scanInstances
            ),
            getEpidemiologyEvent(
                EXPOSURE_WINDOW_POSITIVE_TEST,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false,
                version = 3,
                riskScore = 0.4272038640881409,
                isConsideredRisky = false,
                scanInstances = exposureWindows[1].scanInstances
            )
        )
    }

    private fun getEpidemiologyEvent(
        epidemiologyEventType: EpidemiologyEventType,
        testKitType: VirologyTestKitType? = null,
        requiresConfirmatoryTest: Boolean? = null,
        version: Int,
        riskScore: Double,
        isConsideredRisky: Boolean,
        scanInstances: List<ScanInstance>
    ) = EpidemiologyEventWithType(
        type = epidemiologyEventType,
        version = version,
        payload = EpidemiologyEventPayload(
            testType = testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            date = testAppContext.clock.instant(),
            infectiousness = HIGH,
            scanInstances = scanInstances.map {
                EpidemiologyEventPayloadScanInstance(
                    it.minAttenuationDb,
                    it.secondsSinceLastScan,
                    it.typicalAttenuationDb
                )
            },
            riskCalculationVersion = 2,
            riskScore = riskScore,
            isConsideredRisky = isConsideredRisky
        )
    )
}
