package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import com.google.android.gms.nearby.exposurenotification.Infectiousness
import com.google.android.gms.nearby.exposurenotification.ScanInstance
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.MetadataProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.DayRisk
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import java.time.Instant
import kotlin.test.assertEquals

class SubmitEpidemiologyDataTest {

    private val mockEpidemiologyDataApi = mockk<EpidemiologyDataApi>(relaxed = true)
    private val mockMetadataProvider = mockk<MetadataProvider>()
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxed = true)

    private val testSubject =
        SubmitEpidemiologyData(mockMetadataProvider, mockEpidemiologyDataApi, submitFakeExposureWindows)

    private val dayRisk = DayRisk(startOfDayMillis = 123L, calculatedRisk = 10.0, riskCalculationVersion = 2)
    private val exposureWindow = ExposureWindow.Builder().setInfectiousness(Infectiousness.HIGH).build()
    private val exposureWindowWithRisk = SubmitEpidemiologyData.ExposureWindowWithRisk(dayRisk, exposureWindow)

    @Before
    fun setUp() {
        coEvery { mockEpidemiologyDataApi.submitEpidemiologyData(any()) } returns Unit
        every { mockMetadataProvider.getMetadata() } returns Metadata("", "", "", "")
    }

    @Test
    fun `when given a list of exposure windows and associated risk api is called`() = runBlocking {
        val exposureWindowsWithRisk = listOf(exposureWindowWithRisk)

        testSubject.invoke(exposureWindowsWithRisk)

        coVerify(timeout = 1000) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
        verify { submitFakeExposureWindows(EXPOSURE_WINDOW, exposureWindowsWithRisk.size) }
    }

    @Test
    fun `when given an empty list api is not called but fake submission is called`() = runBlocking {
        testSubject.invoke(listOf())

        coVerify(exactly = 0, timeout = 1000) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
        verify { submitFakeExposureWindows(EXPOSURE_WINDOW, 0) }
    }

    @Test
    fun `when call to empty api throws exception fake exposure window submission is still performed`() = runBlocking {
        coEvery { mockEpidemiologyDataApi.submitEpidemiologyData(any()) } throws Exception()

        val exposureWindowsWithRisk = listOf(exposureWindowWithRisk)

        testSubject.invoke(exposureWindowsWithRisk)

        verify { submitFakeExposureWindows(EXPOSURE_WINDOW, exposureWindowsWithRisk.size) }
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
            ScanInstance.Builder().setSecondsSinceLastScan(0).setMinAttenuationDb(1).setTypicalAttenuationDb(1).build()
        val exposureWindow =
            ExposureWindow.Builder().setInfectiousness(Infectiousness.HIGH).setScanInstances(listOf(scanInstance))
                .build()
        val exposureWindowWithRisk = SubmitEpidemiologyData.ExposureWindowWithRisk(dayRisk, exposureWindow)

        val event = EpidemiologyEvent(
            EpidemiologyEventType.EXPOSURE_WINDOW, 1,
            EpidemiologyEventPayload(
                Instant.parse("2020-11-18T13:20:36.875Z"),
                uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.fromInt(2),
                listOf(EpidemiologyEventPayloadScanInstance(1, 0, 1)),
                10.0,
                2
            )
        )

        assertEquals(event, exposureWindowWithRisk.convert(EpidemiologyEventType.EXPOSURE_WINDOW))
    }
}
