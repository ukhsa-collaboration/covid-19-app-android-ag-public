package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.MetadataProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW_AFTER_POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows

class SubmitEpidemiologyDataTest {

    private val mockEpidemiologyDataApi = mockk<EpidemiologyDataApi>(relaxed = true)
    private val mockMetadataProvider = mockk<MetadataProvider>()
    private val submitFakeExposureWindows = mockk<SubmitFakeExposureWindows>(relaxed = true)
    private val testCoroutineScope = TestCoroutineScope()
    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private val testSubject =
        SubmitEpidemiologyData(
            mockMetadataProvider,
            mockEpidemiologyDataApi,
            submitFakeExposureWindows,
            testCoroutineScope,
            testCoroutineDispatcher
        )

    private val epidemiologyEvent = EpidemiologyEvent(
        version = 1,
        payload = EpidemiologyEventPayload(
            date = Instant.now(),
            infectiousness = HIGH,
            scanInstances = listOf(),
            riskScore = 10.0,
            riskCalculationVersion = 2
        )
    )

    private val metaData = Metadata("", "", "", "")

    private val epidemiologyRequest = EpidemiologyRequest(
        metadata = metaData,
        events = listOf(epidemiologyEvent.toEpidemiologyEventWithType(EpidemiologyEventType.EXPOSURE_WINDOW))
    )

    @Before
    fun setUp() {
        coEvery { mockEpidemiologyDataApi.submitEpidemiologyData(any()) } returns Unit
        every { mockMetadataProvider.getMetadata() } returns metaData
    }

    @Test
    fun `when given a list of exposure events and event type api is called`() =
        testCoroutineScope.runBlockingTest {
            val epidemiologyEvents = listOf(epidemiologyEvent)

            testSubject.invoke(epidemiologyEvents, epidemiologyEventType = EpidemiologyEventType.EXPOSURE_WINDOW)

            coVerify(exactly = 1) { mockEpidemiologyDataApi.submitEpidemiologyData(epidemiologyRequest) }
            verify(exactly = 1) { submitFakeExposureWindows(EXPOSURE_WINDOW, epidemiologyEvents.size) }
        }

    @Test
    fun `when given an empty list api is not called but fake submission is called`() =
        testCoroutineScope.runBlockingTest {
            testSubject.invoke(listOf(), epidemiologyEventType = EpidemiologyEventType.EXPOSURE_WINDOW)

            coVerify(exactly = 0) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
            verify { submitFakeExposureWindows(EXPOSURE_WINDOW, 0) }
        }

    @Test
    fun `when call to empty api throws exception fake exposure window submission is still performed`() =
        testCoroutineScope.runBlockingTest {
            coEvery { mockEpidemiologyDataApi.submitEpidemiologyData(any()) } throws Exception()

            val exposureWindowsWithRisk = listOf(epidemiologyEvent)

            testSubject.invoke(exposureWindowsWithRisk, epidemiologyEventType = EpidemiologyEventType.EXPOSURE_WINDOW)

            coVerify(exactly = 1) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
            verify { submitFakeExposureWindows(EXPOSURE_WINDOW, exposureWindowsWithRisk.size) }
        }

    @Test
    fun `when test positive correct fake call is made with exposure window after positive`() =
        testCoroutineScope.runBlockingTest {
            testSubject.invoke(listOf(), epidemiologyEventType = EpidemiologyEventType.EXPOSURE_WINDOW_POSITIVE_TEST)

            coVerify(exactly = 0) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
            verify { submitFakeExposureWindows(EXPOSURE_WINDOW_AFTER_POSITIVE, 0) }
        }
}
