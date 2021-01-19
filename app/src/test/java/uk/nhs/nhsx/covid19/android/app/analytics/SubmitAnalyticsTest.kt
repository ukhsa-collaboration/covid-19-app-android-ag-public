package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Instant
import kotlin.test.assertEquals

class SubmitAnalyticsTest {

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxed = true)
    private val analyticsApi = mockk<AnalyticsApi>(relaxed = true)
    private val groupAnalyticsEvents = mockk<GroupAnalyticsEvents>(relaxed = true)
    private val migrateMetricsLogStorageToLogStorage =
        mockk<MigrateMetricsLogStorageToLogStorage>(relaxed = true)

    private val testSubject = SubmitAnalytics(
        analyticsLogStorage,
        analyticsApi,
        groupAnalyticsEvents,
        migrateMetricsLogStorageToLogStorage
    )

    @Test
    fun `when grouping analytics events throws an exception then data should not be submitted`() =
        runBlocking {
            coEvery { groupAnalyticsEvents.invoke() } throws Exception()

            testSubject.invoke()

            verify { migrateMetricsLogStorageToLogStorage.invoke() }

            coVerify(exactly = 0) { analyticsApi.submitAnalytics(any()) }
        }

    @Test
    fun `when grouping analytics events returns an empty list then data should not be submitted`() =
        runBlocking {
            coEvery { groupAnalyticsEvents.invoke() } returns Success(listOf())

            testSubject.invoke()

            verify { migrateMetricsLogStorageToLogStorage.invoke() }

            coVerify(exactly = 0) { analyticsApi.submitAnalytics(any()) }
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `cancelling task should not prevent it from sending all the analytics data`() =
        runBlocking {
            coEvery { groupAnalyticsEvents.invoke() } returns stubAnalyticsPayload(2)

            val testCoroutineScope = TestCoroutineScope()
            val job = testCoroutineScope.launch {
                testSubject.invoke(
                    onAfterSubmission = {
                        delay(100)
                    }
                )
            }
            job.cancel()
            testCoroutineScope.advanceTimeBy(300)

            coVerifyOrder {
                analyticsLogStorage.remove(any(), any())
                analyticsApi.submitAnalytics(any())
                analyticsLogStorage.remove(any(), any())
                analyticsApi.submitAnalytics(any())
            }
        }

    @Test
    fun `when grouping analytics events returns multiple payloads then payloads should be submitted`() =
        runBlocking {
            coEvery { groupAnalyticsEvents.invoke() } returns stubAnalyticsPayload(2)

            val result = testSubject.invoke()

            assertEquals(Success(Unit), result)

            verify { migrateMetricsLogStorageToLogStorage.invoke() }

            coVerify(exactly = 2) { analyticsApi.submitAnalytics(any()) }

            coVerify(exactly = 2) { analyticsLogStorage.remove(startDate, endDate) }
        }

    @Test
    fun `when grouping analytics events returns payloads but data submission throws an exception then analytics logs are removed anyway`() =
        runBlocking {
            val testException = Exception()
            coEvery { groupAnalyticsEvents.invoke() } returns stubAnalyticsPayload(3)

            coEvery { analyticsApi.submitAnalytics(any()) } throws testException

            val result = testSubject.invoke()

            assertEquals(Success(Unit), result)

            verify(exactly = 3) { analyticsLogStorage.remove(startDate, endDate) }
        }

    private val startDate = Instant.parse("2020-09-25T00:00:00Z")

    private val endDate = Instant.parse("2020-09-26T00:00:00Z")

    private fun stubAnalyticsPayload(size: Int) =
        Success(
            mutableListOf<AnalyticsPayload>().apply {
                repeat(size) {
                    add(
                        AnalyticsPayload(
                            analyticsWindow = AnalyticsWindow(
                                startDate = startDate.toISOSecondsFormat(),
                                endDate = endDate.toISOSecondsFormat()
                            ),
                            includesMultipleApplicationVersions = false,
                            metadata = Metadata(
                                deviceModel = "",
                                latestApplicationVersion = "",
                                operatingSystemVersion = "",
                                postalDistrict = "",
                                localAuthority = ""
                            ),
                            metrics = Metrics()
                        )
                    )
                }
            }
        )
}
