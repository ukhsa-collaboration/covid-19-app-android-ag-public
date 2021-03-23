package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.CalculateMissingSubmissionDays.Companion.SUBMISSION_LOG_CHECK_RANGE_MAX
import uk.nhs.nhsx.covid19.android.app.analytics.ConsumeOldestAnalyticsResult.Consumed
import uk.nhs.nhsx.covid19.android.app.analytics.ConsumeOldestAnalyticsResult.NothingToConsume
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Instant

class SubmitAnalyticsTest {

    private val analyticsApi = mockk<AnalyticsApi>(relaxUnitFun = true)
    private val migrateMetricsLogStorageToLogStorage = mockk<MigrateMetricsLogStorageToLogStorage>(relaxUnitFun = true)
    private val analyticsSubmissionLogStorage = mockk<AnalyticsSubmissionLogStorage>(relaxUnitFun = true)
    private val consumeOldestAnalytics = mockk<ConsumeOldestAnalytics>()
    private val createAnalyticsPayload = mockk<CreateAnalyticsPayload>()

    private val testSubject = SubmitAnalytics(
        analyticsApi,
        migrateMetricsLogStorageToLogStorage,
        analyticsSubmissionLogStorage,
        consumeOldestAnalytics,
        createAnalyticsPayload
    )

    @Before
    fun setupMocks() {
        every { createAnalyticsPayload.invoke(any()) } returns payload
    }

    @Test
    fun `when consuming events throws an exception then data should not be submitted`() =
        runBlocking {
            coEvery { consumeOldestAnalytics.invoke() } throws Exception()

            testSubject.invoke()

            verify { migrateMetricsLogStorageToLogStorage.invoke() }

            coVerify(exactly = 0) { analyticsApi.submitAnalytics(any()) }
        }

    @Test
    fun `when consuming events returns null then data should not be submitted`() =
        runBlocking {
            coEvery { consumeOldestAnalytics.invoke() } returns NothingToConsume

            testSubject.invoke()

            verify { migrateMetricsLogStorageToLogStorage.invoke() }

            coVerify(exactly = 0) { analyticsApi.submitAnalytics(any()) }
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `cancelling task should not prevent it from sending all the analytics data`() =
        runBlocking {
            coEvery { consumeOldestAnalytics.invoke() } returnsMany listOf(Consumed(group), Consumed(group), NothingToConsume)

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
                analyticsApi.submitAnalytics(any())
                analyticsApi.submitAnalytics(any())
            }
        }

    @Test
    fun `when consuming events returns multiple groups then payloads should be submitted for all of them`() =
        runBlocking {
            coEvery { consumeOldestAnalytics.invoke() } returnsMany listOf(Consumed(group), Consumed(group), NothingToConsume)

            val result = testSubject.invoke()

            assertEquals(Success(Unit), result)

            verify { migrateMetricsLogStorageToLogStorage.invoke() }
            verify(exactly = 2) { createAnalyticsPayload.invoke(group) }
            coVerify(exactly = 2) { analyticsApi.submitAnalytics(payload) }
            verify(exactly = 2) { analyticsSubmissionLogStorage.addDate(analyticsWindow.startDateToLocalDate()) }
            verify(exactly = 2) {
                analyticsSubmissionLogStorage.removeBeforeOrEqual(
                    analyticsWindow.startDateToLocalDate().minusDays(SUBMISSION_LOG_CHECK_RANGE_MAX.toLong())
                )
            }
        }

    @Test
    fun `when consuming events returns 3 groups but first submission throws an exception then the task is aborted`() =
        runBlocking {
            val testException = Exception()
            coEvery { consumeOldestAnalytics.invoke() } returnsMany listOf(Consumed(group), Consumed(group), NothingToConsume)

            coEvery { analyticsApi.submitAnalytics(any()) } throws testException

            val result = testSubject.invoke()

            assertTrue(result is Failure)

            verify(exactly = 1) { createAnalyticsPayload.invoke(group) }
            verify(exactly = 0) { analyticsSubmissionLogStorage.addDate(any()) }
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
}
