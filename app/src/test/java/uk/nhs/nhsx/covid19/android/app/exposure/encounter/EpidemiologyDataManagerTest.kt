package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationWorkTest.Companion.getExposureWindowsWithRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent

import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.ExposureWindowWithRisk
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.PartitionExposureWindowsResult
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import java.time.Instant

class EpidemiologyDataManagerTest {

    private val randomLimiterFilter = mockk<RandomNonRiskyExposureWindowsLimiter>()
    private val submitEpidemiologyData = mockk<SubmitEpidemiologyData>(relaxUnitFun = true)
    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>(relaxUnitFun = true)
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxUnitFun = true)

    private val testSubject = EpidemiologyDataManager(
        randomLimiterFilter,
        epidemiologyEventProvider,
        submitEpidemiologyData,
        submitFakeExposureWindows
    )

    @Test
    fun `calling without exposure windows should submit fake exposure windows`() = runBlocking {
        every { randomLimiterFilter.isAllowed() } returns true

        testSubject.storeAndSubmit(
            PartitionExposureWindowsResult(
                riskyExposureWindows = emptyList(),
                nonRiskyExposureWindows = emptyList()
            )
        )

        verify(exactly = 0) { epidemiologyEventProvider.addRiskyEpidemiologyEvents(any()) }
        verify(exactly = 0) { submitEpidemiologyData.submit(any()) }
        verify(exactly = 1) { submitFakeExposureWindows.invoke() }
    }

    @Test
    fun `calling with exposure windows should store and submit epidemiology events when non risky windows allowed`() = runBlocking {
        val riskyExposureWindows = getExposureWindowsWithRisk(isConsideredRisky = true, 8.0, 10.0)
        val nonRiskyExposureWindows = getExposureWindowsWithRisk(isConsideredRisky = false, 0.1, 0.2)

        every { randomLimiterFilter.isAllowed() } returns true

        testSubject.storeAndSubmit(
            PartitionExposureWindowsResult(
                riskyExposureWindows = riskyExposureWindows,
                nonRiskyExposureWindows = nonRiskyExposureWindows
            )
        )

        val expectedRiskyEpidemiologyEvents = getEpidemiologyEvents(riskyExposureWindows, isConsideredRisky = true)
        val expectedNonRiskyEpidemiologyEvents = getEpidemiologyEvents(nonRiskyExposureWindows, isConsideredRisky = false)
        val expectedEpidemiologyEvents = expectedRiskyEpidemiologyEvents.plus(expectedNonRiskyEpidemiologyEvents)

        verify(exactly = 1) { epidemiologyEventProvider.addRiskyEpidemiologyEvents(expectedRiskyEpidemiologyEvents) }
        verify(exactly = 1) { epidemiologyEventProvider.addNonRiskyEpidemiologyEvents(expectedNonRiskyEpidemiologyEvents, storageLimit = 15) }
        verify(exactly = 1) { submitEpidemiologyData.submit(expectedEpidemiologyEvents) }
        verify(exactly = 0) { submitFakeExposureWindows.invoke(any()) }
    }

    @Test
    fun `calling with exposure windows should store and submit only risky epidemiology events when non risky windows disallowed`() = runBlocking {
        val riskyExposureWindows = getExposureWindowsWithRisk(isConsideredRisky = true, 8.0, 10.0)
        val nonRiskyExposureWindows = getExposureWindowsWithRisk(isConsideredRisky = false, 0.1, 0.2)

        every { randomLimiterFilter.isAllowed() } returns false

        testSubject.storeAndSubmit(
            PartitionExposureWindowsResult(
                riskyExposureWindows = riskyExposureWindows,
                nonRiskyExposureWindows = nonRiskyExposureWindows
            )
        )

        val expectedRiskyEpidemiologyEvents = getEpidemiologyEvents(riskyExposureWindows, isConsideredRisky = true)

        verify(exactly = 1) { epidemiologyEventProvider.addRiskyEpidemiologyEvents(expectedRiskyEpidemiologyEvents) }
        verify(exactly = 0) { epidemiologyEventProvider.addNonRiskyEpidemiologyEvents(any(), any()) }
        verify(exactly = 1) { submitEpidemiologyData.submit(expectedRiskyEpidemiologyEvents) }
        verify(exactly = 0) { submitFakeExposureWindows.invoke(any()) }
    }

    @Test
    fun `store and submit all risky epidemiology events`() = runBlocking {
        val risks = (1..20).map { it.toDouble() }
        val riskyExposureWindows = getExposureWindowsWithRisk(isConsideredRisky = true, *risks.toDoubleArray())

        every { randomLimiterFilter.isAllowed() } returns true

        testSubject.storeAndSubmit(
            PartitionExposureWindowsResult(
                riskyExposureWindows = riskyExposureWindows,
                nonRiskyExposureWindows = emptyList()
            )
        )

        val expectedEpidemiologyEvents = getEpidemiologyEvents(riskyExposureWindows, isConsideredRisky = true)

        verify(exactly = 1) { epidemiologyEventProvider.addRiskyEpidemiologyEvents(expectedEpidemiologyEvents) }
        verify(exactly = 1) { submitEpidemiologyData.submit(expectedEpidemiologyEvents) }
        verify(exactly = 0) { submitFakeExposureWindows.invoke(any()) }
    }

    @Test
    fun `store and submit no more than 15 non risky epidemiology events`() = runBlocking {
        val risks = (1..20).map { it.toDouble() }
        val nonRiskyExposureWindows = getExposureWindowsWithRisk(isConsideredRisky = false, *risks.toDoubleArray())

        every { randomLimiterFilter.isAllowed() } returns true

        testSubject.storeAndSubmit(
            PartitionExposureWindowsResult(
                riskyExposureWindows = emptyList(),
                nonRiskyExposureWindows = nonRiskyExposureWindows
            )
        )

        val expectedEpidemiologyEvents = getEpidemiologyEvents(nonRiskyExposureWindows, isConsideredRisky = false)
            .takeLast(15)

        verify(exactly = 1) { epidemiologyEventProvider.addNonRiskyEpidemiologyEvents(expectedEpidemiologyEvents, storageLimit = 15) }
        verify(exactly = 1) { submitEpidemiologyData.submit(expectedEpidemiologyEvents) }
        verify(exactly = 0) { submitFakeExposureWindows.invoke(any()) }
    }

    private fun getEpidemiologyEvents(
        exposureWindows: List<ExposureWindowWithRisk>,
        isConsideredRisky: Boolean
    ): List<EpidemiologyEvent> =
        exposureWindows.map {
            EpidemiologyEvent(
                payload = EpidemiologyEventPayload(
                    date = Instant.ofEpochMilli(it.startOfDayMillis),
                    infectiousness = uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.fromInt(
                        it.exposureWindow.infectiousness
                    ),
                    scanInstances = it.exposureWindow.scanInstances.map { scanInstance ->
                        EpidemiologyEventPayloadScanInstance(
                            minimumAttenuation = scanInstance.minAttenuationDb,
                            secondsSinceLastScan = scanInstance.secondsSinceLastScan,
                            typicalAttenuation = scanInstance.typicalAttenuationDb
                        )
                    },
                    riskScore = it.calculatedRisk,
                    riskCalculationVersion = it.riskCalculationVersion,
                    isConsideredRisky = isConsideredRisky
                )
            )
        }
}
