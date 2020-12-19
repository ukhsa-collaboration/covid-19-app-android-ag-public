package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class GroupAnalyticsEventsTest {

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxed = true)
    private val metadataProvider = mockk<MetadataProvider>(relaxed = true)
    private val updateStatusStorage = mockk<UpdateStatusStorage>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-09-29T00:05:00.00Z"), ZoneOffset.UTC)

    private val testSubject = GroupAnalyticsEvents(
        analyticsLogStorage,
        metadataProvider,
        updateStatusStorage,
        GetAnalyticsWindow(fixedClock),
        fixedClock
    )

    @Before
    fun setUp() {
        every { metadataProvider.getMetadata() } returns stubMetadata()
    }

    @Test
    fun `failure on reading events log returns failure`() = runBlocking {
        val exception = Exception()

        every { analyticsLogStorage.value } throws exception

        val actual = testSubject.invoke()

        val expected = Failure(exception)

        assertEquals(expected, actual)
    }

    @Test
    fun `filter out the current window`() = runBlocking {
        every { analyticsLogStorage.value } returns
            listOf(currentWindowLogEntries, listOf(lastWindowLogEntry)).flatten()

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
                    metrics = Metrics(
                        receivedPositiveTestResult = 1
                    )
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `group events in two analytics windows`() =
        runBlocking {
            every { analyticsLogStorage.value } returns
                listOf(currentWindowLogEntries, listOf(lastWindowLogEntry), listOf(oldWindowLogEntry)).flatten()

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
                        metrics = Metrics(
                            receivedPositiveTestResult = 1
                        )
                    ),
                    AnalyticsPayload(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-26T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat()
                        ),
                        includesMultipleApplicationVersions = false,
                        metadata = stubMetadata(),
                        metrics = Metrics(
                            completedQuestionnaireAndStartedIsolation = 1
                        )
                    )
                )
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `add canceledCheckIn for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(CANCELED_CHECK_IN))
    }

    @Test
    fun `add checkedIn for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(QR_CODE_CHECK_IN))
    }

    @Test
    fun `add completedQuestionnaireAndStartedIsolation for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION))
    }

    @Test
    fun `add completedQuestionnaireButDidNotStartIsolation for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION))
    }

    @Test
    fun `add cumulativeDownloadBytes for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(UpdateNetworkStats(downloadedBytes = 25, uploadedBytes = null))
    }

    @Test
    fun `add cumulativeUploadBytes for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(UpdateNetworkStats(downloadedBytes = null, uploadedBytes = 15))
    }

    @Test
    fun `add encounterDetectionPausedBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(encounterDetectionPausedBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add hasHadRiskyContactBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasHadRiskyContactBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add hasSelfDiagnosedPositiveBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasSelfDiagnosedPositiveBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add isIsolatingBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(isIsolatingBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add receivedNegativeTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(NEGATIVE_RESULT_RECEIVED))
    }

    @Test
    fun `add receivedPositiveTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(POSITIVE_RESULT_RECEIVED))
    }

    @Test
    fun `add receivedVoidTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(VOID_RESULT_RECEIVED))
    }

    @Test
    fun `add receivedRiskyContactNotification for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(RECEIVED_RISKY_CONTACT_NOTIFICATION))
    }

    @Test
    fun `add startedIsolation for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(Event(STARTED_ISOLATION))
    }

    @Test
    fun `add receivedVoidTestResultEnteredManually for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(ResultReceived(VOID, INSIDE_APP))
    }

    @Test
    fun `add receivedPositiveTestResultEnteredManually for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(ResultReceived(POSITIVE, INSIDE_APP))
    }

    @Test
    fun `add receivedNegativeTestResultEnteredManually for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(ResultReceived(NEGATIVE, INSIDE_APP))
    }

    @Test
    fun `add receivedVoidTestResultViaPolling for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(ResultReceived(VOID, OUTSIDE_APP))
    }

    @Test
    fun `add receivedPositiveTestResultViaPolling for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(ResultReceived(POSITIVE, OUTSIDE_APP))
    }

    @Test
    fun `add receivedNegativeTestResultViaPolling for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(ResultReceived(NEGATIVE, OUTSIDE_APP))
    }

    @Test
    fun `add runningNormallyBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(runningNormallyBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add totalBackgroundTasks for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks()
            )
        )
    }

    @Test
    fun `add hasSelfDiagnosedBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasSelfDiagnosedBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add hasTestedPositiveBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasTestedPositiveBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add isIsolatingForSelfDiagnosedBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(isIsolatingForSelfDiagnosedBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add isIsolatingForTestedPositiveBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(isIsolatingForTestedPositiveBackgroundTick = true)
            )
        )
    }

    @Test
    fun `add isIsolatingForHadRiskyContactBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(
                    isIsolatingForHadRiskyContactBackgroundTick = true
                )
            )
        )
    }

    private fun `test aggregation of analytics metrics`(analyticsLogItem: AnalyticsLogItem) = runBlocking {

        val logEntry1 = AnalyticsLogEntry(
            instant = Instant.parse("2020-09-28T00:00:00Z"),
            logItem = analyticsLogItem
        )
        val logEntries1 = listOf(logEntry1, logEntry1, logEntry1, logEntry1)

        val logEntry2 = AnalyticsLogEntry(
            instant = Instant.parse("2020-09-28T23:59:59Z"),
            logItem = analyticsLogItem
        )
        val logEntries2 = listOf(logEntry2, logEntry2, logEntry2, logEntry2, logEntry2)

        val analyticsLog = listOf(logEntries1, logEntries2).flatten()

        every { analyticsLogStorage.value } returns analyticsLog

        val actual = testSubject.invoke()

        val analyticsLogAsMetrics = analyticsLog.toMetrics()

        val expected = Success(
            listOf(
                AnalyticsPayload(
                    analyticsWindow = AnalyticsWindow(
                        startDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat(),
                        endDate = Instant.parse("2020-09-29T00:00:00Z").toISOSecondsFormat()
                    ),
                    includesMultipleApplicationVersions = false,
                    metadata = stubMetadata(),
                    metrics = analyticsLogAsMetrics
                )
            )
        )

        assertEquals(expected, actual)
    }

    private val currentWindowLogEntries = listOf(
        AnalyticsLogEntry(
            instant = Instant.parse("2020-09-29T00:00:00Z"),
            logItem = Event(QR_CODE_CHECK_IN)
        ),
        AnalyticsLogEntry(
            instant = Instant.parse("2020-09-29T00:00:00Z"),
            logItem = Event(CANCELED_CHECK_IN)
        )
    )

    private val lastWindowLogEntry = AnalyticsLogEntry(
        instant = Instant.parse("2020-09-27T00:00:00Z"),
        logItem = Event(POSITIVE_RESULT_RECEIVED)
    )

    private val oldWindowLogEntry = AnalyticsLogEntry(
        instant = Instant.parse("2020-09-26T00:00:00Z"),
        logItem = Event(COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION)
    )

    private fun stubMetadata() = Metadata(
        deviceModel = "null null",
        latestApplicationVersion = BuildConfig.VERSION_NAME_SHORT,
        operatingSystemVersion = "0",
        postalDistrict = ""
    )
}
