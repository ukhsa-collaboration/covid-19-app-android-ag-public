package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.CalculateMissingSubmissionDays.Companion.SUBMISSION_LOG_CHECK_RANGE_MAX
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

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxUnitFun = true)
    private val analyticsApi = mockk<AnalyticsApi>(relaxUnitFun = true)
    private val groupAnalyticsEvents = mockk<GroupAnalyticsEvents>()
    private val migrateMetricsLogStorageToLogStorage = mockk<MigrateMetricsLogStorageToLogStorage>(relaxUnitFun = true)
    private val analyticsSubmissionLogStorage = mockk<AnalyticsSubmissionLogStorage>(relaxUnitFun = true)
    private val createAnalyticsPayload = mockk<CreateAnalyticsPayload>()

    private val testSubject = SubmitAnalytics(
        analyticsLogStorage,
        analyticsApi,
        groupAnalyticsEvents,
        migrateMetricsLogStorageToLogStorage,
        analyticsSubmissionLogStorage,
        createAnalyticsPayload,
    )

    @Before
    fun setupMocks() {
        every { createAnalyticsPayload.invoke(any()) } returns payload
    }

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
            coEvery { groupAnalyticsEvents.invoke() } returns stubAnalyticsGroups(2)

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
    fun `when grouping analytics events returns multiple groups then payloads should be submitted for all of them`() =
        runBlocking {
            coEvery { groupAnalyticsEvents.invoke() } returns stubAnalyticsGroups(2)

            val result = testSubject.invoke()

            assertEquals(Success(Unit), result)

            verify { migrateMetricsLogStorageToLogStorage.invoke() }
            verify(exactly = 2) { createAnalyticsPayload.invoke(group) }
            coVerify(exactly = 2) { analyticsApi.submitAnalytics(payload) }
            verify(exactly = 2) { analyticsSubmissionLogStorage.add(analyticsWindow.startDateToLocalDate()) }
            verify(exactly = 2) {
                analyticsSubmissionLogStorage.removeBeforeOrEqual(
                    analyticsWindow.startDateToLocalDate().minusDays(SUBMISSION_LOG_CHECK_RANGE_MAX.toLong())
                )
            }
            coVerify(exactly = 2) { analyticsLogStorage.remove(startDate, endDate) }
        }

    @Test
    fun `when grouping returns 3 groups but first submission throws an exception then the task is aborted`() =
        runBlocking {
            val testException = Exception()
            coEvery { groupAnalyticsEvents.invoke() } returns stubAnalyticsGroups(3)

            coEvery { analyticsApi.submitAnalytics(any()) } throws testException

            val result = testSubject.invoke()

            assertEquals(Success(Unit), result)

            verify(exactly = 1) { analyticsLogStorage.remove(startDate, endDate) }
            verify(exactly = 1) { createAnalyticsPayload.invoke(group) }
            verify(exactly = 0) { analyticsSubmissionLogStorage.add(any()) }
            verify(exactly = 0) { analyticsSubmissionLogStorage.removeBeforeOrEqual(any()) }
        }

    private val startDate = Instant.parse("2020-09-25T00:00:00Z")
    private val endDate = Instant.parse("2020-09-26T00:00:00Z")
    private val analyticsWindow = AnalyticsWindow(
        startDate = startDate.toISOSecondsFormat(),
        endDate = endDate.toISOSecondsFormat()
    )

    private val group = AnalyticsEventsGroup(
        analyticsWindow = analyticsWindow,
        entries = listOf()
    )

    private val payload = AnalyticsPayload(
        analyticsWindow = analyticsWindow,
        includesMultipleApplicationVersions = false,
        metadata = Metadata("", "", "", "", ""),
        metrics = Metrics()
    )

    private fun stubAnalyticsGroups(size: Int) =
        Success(
            mutableListOf<AnalyticsEventsGroup>().apply {
                repeat(size) {
                    add(group)
                }
            }
        )
}
