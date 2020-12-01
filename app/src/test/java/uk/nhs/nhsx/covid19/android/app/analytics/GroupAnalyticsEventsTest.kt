package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class GroupAnalyticsEventsTest {

    private val analyticsMetricsLogStorage = mockk<AnalyticsMetricsLogStorage>(relaxed = true)
    private val metadataProvider = mockk<MetadataProvider>(relaxed = true)
    private val updateStatusStorage = mockk<UpdateStatusStorage>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-09-29T00:05:00.00Z"), ZoneOffset.UTC)

    private val getAnalyticsWindow = GetAnalyticsWindow(fixedClock)

    private val testSubject = GroupAnalyticsEvents(
        analyticsMetricsLogStorage,
        metadataProvider,
        updateStatusStorage,
        getAnalyticsWindow,
        fixedClock
    )

    @Before
    fun setUp() {
        every { metadataProvider.getMetadata() } returns stubMetadata()
    }

    @Test
    fun `failure on reading events log returns failure`() = runBlocking {
        val exception = Exception()

        every { analyticsMetricsLogStorage.value } throws exception

        val actual = testSubject.invoke()

        val expected = Failure(exception)

        assertEquals(expected, actual)
    }

    @Test
    fun `filter out the current window`() = runBlocking {
        every { analyticsMetricsLogStorage.value } returns currentWindowMetrics.plus(lastWindowMetrics)

        val actual = testSubject.invoke()

        val expected = Success(
            listOf(
                AnalyticsPayload(
                    analyticsWindow = AnalyticsWindow(
                        startDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat(),
                        endDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat()
                    ),
                    includesMultipleApplicationVersions = false,
                    metadata = stubMetadata(),
                    metrics = Metrics()
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `group events in two analytics windows`() =
        runBlocking {
            every { analyticsMetricsLogStorage.value } returns currentWindowMetrics.plus(
                lastWindowMetrics
            ).plus(oldWindowMetrics)

            val actual = testSubject.invoke()

            val expected = Success(
                listOf(
                    AnalyticsPayload(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat()
                        ),
                        includesMultipleApplicationVersions = false,
                        metadata = stubMetadata(),
                        metrics = Metrics()
                    ),
                    AnalyticsPayload(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-26T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat()
                        ),
                        includesMultipleApplicationVersions = false,
                        metadata = stubMetadata(),
                        metrics = Metrics()
                    )
                )
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `add canceledCheckIn for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.canceledCheckIn = value }
    }

    @Test
    fun `add checkedIn for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.checkedIn = value }
    }

    @Test
    fun `add completedOnboarding for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.completedOnboarding = value }
    }

    @Test
    fun `add completedQuestionnaireAndStartedIsolation for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.completedQuestionnaireAndStartedIsolation = value }
    }

    @Test
    fun `add completedQuestionnaireButDidNotStartIsolation for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.completedQuestionnaireButDidNotStartIsolation = value }
    }

    @Test
    fun `add cumulativeDownloadBytes for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.cumulativeDownloadBytes = value }
    }

    @Test
    fun `add cumulativeUploadBytes for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.cumulativeUploadBytes = value }
    }

    @Test
    fun `add encounterDetectionPausedBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.encounterDetectionPausedBackgroundTick = value }
    }

    @Test
    fun `add hasHadRiskyContactBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.hasHadRiskyContactBackgroundTick = value }
    }

    @Test
    fun `add hasSelfDiagnosedPositiveBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.hasSelfDiagnosedPositiveBackgroundTick = value }
    }

    @Test
    fun `add isIsolatingBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.isIsolatingBackgroundTick = value }
    }

    @Test
    fun `add receivedNegativeTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedNegativeTestResult = value }
    }

    @Test
    fun `add receivedPositiveTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedPositiveTestResult = value }
    }

    @Test
    fun `add receivedVoidTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedVoidTestResult = value }
    }

    @Test
    fun `add receivedVoidTestResultEnteredManually for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedVoidTestResultEnteredManually = value }
    }

    @Test
    fun `add receivedPositiveTestResultEnteredManually for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedPositiveTestResultEnteredManually = value }
    }

    @Test
    fun `add receivedNegativeTestResultEnteredManually for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedNegativeTestResultEnteredManually = value }
    }

    @Test
    fun `add receivedVoidTestResultViaPolling for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedVoidTestResultViaPolling = value }
    }

    @Test
    fun `add receivedPositiveTestResultViaPolling for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedPositiveTestResultViaPolling = value }
    }

    @Test
    fun `add receivedNegativeTestResultViaPolling for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.receivedNegativeTestResultViaPolling = value }
    }

    @Test
    fun `add runningNormallyBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.runningNormallyBackgroundTick = value }
    }

    @Test
    fun `add totalBackgroundTasks for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.totalBackgroundTasks = value }
    }

    @Test
    fun `add hasSelfDiagnosedBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.hasSelfDiagnosedBackgroundTick = value }
    }

    @Test
    fun `add hasTestedPositiveBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.hasTestedPositiveBackgroundTick = value }
    }

    @Test
    fun `add isIsolatingForSelfDiagnosedBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.isIsolatingForSelfDiagnosedBackgroundTick = value }
    }

    @Test
    fun `add isIsolatingForTestedPositiveBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.isIsolatingForTestedPositiveBackgroundTick = value }
    }

    @Test
    fun `add isIsolatingForHadRiskyContactBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics` { metrics: Metrics, value: Int -> metrics.isIsolatingForHadRiskyContactBackgroundTick = value }
    }

    private fun `test aggregation of analytics metrics`(metricsValueSetter: (Metrics, Int) -> Unit) = runBlocking {
        val entry1 = MetricsLogEntry(
            Metrics().apply { metricsValueSetter(this, 4) },
            Instant.parse("2020-09-28T00:00:00Z")
        )
        val entry2 = MetricsLogEntry(
            Metrics().apply { metricsValueSetter(this, 5) },
            Instant.parse("2020-09-28T23:59:59Z")
        )

        every { analyticsMetricsLogStorage.value } returns listOf(entry1, entry2)

        val actual = testSubject.invoke()

        val expected = Success(
            listOf(
                AnalyticsPayload(
                    analyticsWindow = AnalyticsWindow(
                        startDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat(),
                        endDate = Instant.parse("2020-09-29T00:00:00Z").toISOSecondsFormat()
                    ),
                    includesMultipleApplicationVersions = false,
                    metadata = stubMetadata(),
                    metrics = Metrics().apply { metricsValueSetter(this, 9) }
                )
            )
        )

        assertEquals(expected, actual)
    }

    private val currentWindowMetrics = listOf(
        MetricsLogEntry(
            Metrics(),
            Instant.parse("2020-09-29T00:00:00Z")
        ),
        MetricsLogEntry(
            Metrics(),
            Instant.parse("2020-09-29T23:59:59Z")
        )
    )

    private val lastWindowMetrics = MetricsLogEntry(
        Metrics(),
        Instant.parse("2020-09-27T00:00:00Z")
    )

    private val oldWindowMetrics = MetricsLogEntry(
        Metrics(),
        Instant.parse("2020-09-26T00:00:00Z")
    )

    private fun stubMetadata() = Metadata(
        deviceModel = "null null",
        latestApplicationVersion = BuildConfig.VERSION_NAME_SHORT,
        operatingSystemVersion = "0",
        postalDistrict = ""
    )
}
