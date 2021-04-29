package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.MetadataProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import java.time.Instant

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
        payload = EpidemiologyEventPayload(
            date = Instant.now(),
            infectiousness = HIGH,
            scanInstances = listOf(),
            riskScore = 10.0,
            riskCalculationVersion = 2,
            isConsideredRisky = false
        )
    )

    private val metaData = Metadata("", "", "", "", "")

    private val exposureWindowRequest = EpidemiologyRequest(
        metadata = metaData,
        events = listOf(
            epidemiologyEvent.toEpidemiologyEventWithType(
                EpidemiologyEventType.EXPOSURE_WINDOW,
                eventVersion = 2,
                testKitType = null,
                requiresConfirmatoryTest = null
            )
        )
    )

    private val exposureWindowRequestWithPositiveTest = EpidemiologyRequest(
        metadata = metaData,
        events = listOf(
            epidemiologyEvent.toEpidemiologyEventWithType(
                EpidemiologyEventType.EXPOSURE_WINDOW_POSITIVE_TEST,
                eventVersion = 3,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = true
            )
        )
    )

    @Before
    fun setUp() {
        coEvery { mockEpidemiologyDataApi.submitEpidemiologyData(any()) } returns Unit
        every { mockMetadataProvider.getMetadata() } returns metaData
    }

    @Test
    fun `when submitting a list of exposure events api and fake submission are called`() =
        testCoroutineScope.runBlockingTest {
            val epidemiologyEvents = listOf(epidemiologyEvent)

            testSubject.submit(epidemiologyEvents)

            coVerify(exactly = 1) {
                mockEpidemiologyDataApi.submitEpidemiologyData(exposureWindowRequest)
            }
            verify(exactly = 1) {
                submitFakeExposureWindows(epidemiologyEvents.size)
            }
        }

    @Test
    fun `when submitting a list of exposure events after positive test result api and fake submission are called`() =
        testCoroutineScope.runBlockingTest {
            val epidemiologyEvents = listOf(epidemiologyEvent)

            testSubject.submitAfterPositiveTest(
                epidemiologyEvents,
                LAB_RESULT,
                requiresConfirmatoryTest = true
            )

            coVerify(exactly = 1) {
                mockEpidemiologyDataApi.submitEpidemiologyData(exposureWindowRequestWithPositiveTest)
            }
            verify(exactly = 1) {
                submitFakeExposureWindows(epidemiologyEvents.size)
            }
        }

    @Test
    fun `when given an empty list api is not called but fake submission is called`() =
        testCoroutineScope.runBlockingTest {
            testSubject.submit(listOf())

            coVerify(exactly = 0) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
            verify { submitFakeExposureWindows(0) }
        }

    @Test
    fun `when call to empty api throws exception fake exposure window submission is still performed`() =
        testCoroutineScope.runBlockingTest {
            coEvery { mockEpidemiologyDataApi.submitEpidemiologyData(any()) } throws Exception()

            val exposureWindowsWithRisk = listOf(epidemiologyEvent)

            testSubject.submit(exposureWindowsWithRisk)

            coVerify(exactly = 1) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
            verify { submitFakeExposureWindows(exposureWindowsWithRisk.size) }
        }

    @Test
    fun `when test positive correct fake call is made with exposure window after positive`() =
        testCoroutineScope.runBlockingTest {
            testSubject.submitAfterPositiveTest(
                listOf(),
                testKitType = null,
                requiresConfirmatoryTest = null
            )

            coVerify(exactly = 0) { mockEpidemiologyDataApi.submitEpidemiologyData(any()) }
            verify { submitFakeExposureWindows(0) }
        }
}
