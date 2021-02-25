package uk.nhs.nhsx.covid19.android.app.flow.analytics

import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.SymptomsAndOnsetFlowConfiguration
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW_POSITIVE_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventWithType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import kotlin.test.assertEquals

class ExposureWindowAnalyticsTest : AnalyticsTest() {
    private val riskyContact = RiskyContact(this)
    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)

    private val lastRequest: EpidemiologyRequest
        get() = testAppContext.epidemiologyDataApi.lastRequest().getOrAwaitValue()

    @Test
    fun submitsExposureWindowData_whenUserHasRiskyContact() = runBlocking {
        riskyContact.triggerViaBroadcastReceiver()

        val expectedEvents = listOf(getRiskyEncounterEpidemiologyEvent())
        assertEquals(expectedEvents, lastRequest.events)
    }

    @Test
    fun submitsExposureWindowData_whenUserReceivesPositiveTest() = runBlocking {
        riskyContact.triggerViaBroadcastReceiver()

        manualTestResultEntry.enterPositive(
            LAB_RESULT,
            SymptomsAndOnsetFlowConfiguration()
        )

        val expectedEvents = listOf(getPositiveTestEpidemiologyEvent())
        assertEquals(expectedEvents, lastRequest.events)
    }

    private suspend fun getRiskyEncounterEpidemiologyEvent() = getEpidemiologyEvent(
        EXPOSURE_WINDOW,
        version = 1
    )

    private suspend fun getPositiveTestEpidemiologyEvent() = getEpidemiologyEvent(
        EXPOSURE_WINDOW_POSITIVE_TEST,
        testKitType = LAB_RESULT,
        requiresConfirmatoryTest = false,
        version = 2
    )

    private suspend fun getEpidemiologyEvent(
        epidemiologyEventType: EpidemiologyEventType,
        testKitType: VirologyTestKitType? = null,
        requiresConfirmatoryTest: Boolean? = null,
        version: Int
    ) = EpidemiologyEventWithType(
        type = epidemiologyEventType,
        version = version,
        payload = EpidemiologyEventPayload(
            testType = testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            date = testAppContext.clock.instant(),
            infectiousness = HIGH,
            scanInstances = testAppContext.getExposureNotificationApi().getExposureWindows()
                .first().scanInstances.map {
                    EpidemiologyEventPayloadScanInstance(
                        it.minAttenuationDb,
                        it.secondsSinceLastScan,
                        it.typicalAttenuationDb
                    )
                },
            riskCalculationVersion = 2,
            riskScore = 312.8639553203358
        )
    )
}
