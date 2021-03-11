package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ExposureWindowMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import java.time.Instant
import kotlin.test.assertEquals

class AnalyticsLogStorageTest {

    private val analyticsLogEntryJsonStorage = mockk<AnalyticsLogEntryJsonStorage>(relaxed = true)
    private val moshi = Moshi.Builder()
        .add(InstantAdapter())
        .add(AnalyticsLogStorage.analyticsLogItemAdapter)
        .build()

    private val testSubject = AnalyticsLogStorage(analyticsLogEntryJsonStorage, moshi)

    @Test
    fun `verify empty storage`() {
        every { analyticsLogEntryJsonStorage.value } returns null

        val parsedAnalyticsLogEntries = testSubject.value

        assertEquals(expected = listOf(), actual = parsedAnalyticsLogEntries)
    }

    @Test
    fun `verify corrupted storage`() {
        every { analyticsLogEntryJsonStorage.value } returns "dsfdsfsdfdsfdsf"

        val parsedAnalyticsLogEntries = testSubject.value

        assertEquals(expected = listOf(), actual = parsedAnalyticsLogEntries)
    }

    @Test
    fun `verify serialization`() {
        every { analyticsLogEntryJsonStorage.value } returns ANALYTICS_LOG_ENTRIES_JSON

        val parsedAnalyticsLogEntries = testSubject.value

        assertEquals(expected = ANALYTICS_LOG_ENTRIES, actual = parsedAnalyticsLogEntries)
    }

    @Test
    fun `verify deserialization`() {
        testSubject.value = ANALYTICS_LOG_ENTRIES

        verify { analyticsLogEntryJsonStorage.value = ANALYTICS_LOG_ENTRIES_JSON }
    }

    @Test
    fun `verify migration of test result received with no test kit type`() {
        every { analyticsLogEntryJsonStorage.value } returns RESULT_RECEIVED_NO_TEST_KIT_TYPE_JSON

        val parsedAnalyticsLogEntries = testSubject.value

        val expectedAnalyticsLogEntries = listOf(
            AnalyticsLogEntry(
                instant = Instant.parse("2020-11-18T13:31:32.527Z"),
                logItem = ResultReceived(
                    result = POSITIVE,
                    testKitType = LAB_RESULT,
                    testOrderType = OUTSIDE_APP
                )
            )
        )

        assertEquals(expected = expectedAnalyticsLogEntries, actual = parsedAnalyticsLogEntries)
    }

    @Test
    fun `verify adding new log entry`() {
        testSubject.value = ANALYTICS_LOG_ENTRIES

        val newLogEntry = AnalyticsLogEntry(
            instant = Instant.parse("2020-11-18T13:40:56.333Z"),
            logItem = Event(POSITIVE_RESULT_RECEIVED)
        )

        testSubject.add(newLogEntry)

        val newLogEntryJson =
            """[{"instant":"2020-11-18T13:40:56.333Z","logItem":{"type":"Event","eventType":"POSITIVE_RESULT_RECEIVED"}}]"""

        verify { analyticsLogEntryJsonStorage setProperty "value" value eq(newLogEntryJson) }
    }

    @Test
    fun `verify removal of analytics events that are within the given window`() {
        every { analyticsLogEntryJsonStorage.value } returns ANALYTICS_LOG_ENTRIES_JSON

        testSubject.remove(
            startInclusive = Instant.parse("2020-11-18T13:30:00.000Z"),
            endExclusive = Instant.parse("2020-11-18T13:32:00.000Z")
        )

        val updatedAnalyticsLogEntriesJson =
            """[{"instant":"2020-11-18T13:29:56.385Z","logItem":{"type":"BackgroundTaskCompletion","backgroundTaskTicks":$BACKGROUND_TASK_TICKS}},{"instant":"2020-11-18T13:33:32.123Z","logItem":{"type":"UpdateNetworkStats","downloadedBytes":25,"uploadedBytes":15}},{"instant":"2020-11-18T13:33:33.123Z","logItem":{"type":"ExposureWindowMatched","totalRiskyExposures":1,"totalNonRiskyExposures":2}}]"""
        verify {
            analyticsLogEntryJsonStorage setProperty "value" value eq(updatedAnalyticsLogEntriesJson)
        }
    }

    @Test
    fun `verify no events are removed when none match the specified window`() {
        every { analyticsLogEntryJsonStorage.value } returns ANALYTICS_LOG_ENTRIES_JSON

        testSubject.remove(
            startInclusive = Instant.parse("2020-11-18T13:28:00.000Z"),
            endExclusive = Instant.parse("2020-11-18T13:29:00.000Z")
        )

        verify { analyticsLogEntryJsonStorage setProperty "value" value eq(ANALYTICS_LOG_ENTRIES_JSON) }
    }

    companion object {
        private val ANALYTICS_LOG_ENTRIES = listOf(
            AnalyticsLogEntry(
                instant = Instant.parse("2020-11-18T13:29:56.385Z"),
                logItem = BackgroundTaskCompletion(
                    BackgroundTaskTicks(
                        runningNormallyBackgroundTick = true,
                        isIsolatingBackgroundTick = true,
                        isIsolatingForHadRiskyContactBackgroundTick = true
                    )
                )
            ),
            AnalyticsLogEntry(
                instant = Instant.parse("2020-11-18T13:30:15.120Z"),
                logItem = Event(QR_CODE_CHECK_IN)
            ),
            AnalyticsLogEntry(
                instant = Instant.parse("2020-11-18T13:31:32.527Z"),
                logItem = ResultReceived(result = POSITIVE, testKitType = LAB_RESULT, testOrderType = OUTSIDE_APP)
            ),
            AnalyticsLogEntry(
                instant = Instant.parse("2020-11-18T13:33:32.123Z"),
                logItem = UpdateNetworkStats(downloadedBytes = 25, uploadedBytes = 15)
            ),
            AnalyticsLogEntry(
                instant = Instant.parse("2020-11-18T13:33:33.123Z"),
                logItem = ExposureWindowMatched(totalRiskyExposures = 1, totalNonRiskyExposures = 2)
            )
        )
        private const val BACKGROUND_TASK_TICKS =
            """{"runningNormallyBackgroundTick":true,"isIsolatingBackgroundTick":true,"isIsolatingForHadRiskyContactBackgroundTick":true,"hasSelfDiagnosedPositiveBackgroundTick":false,"isIsolatingForSelfDiagnosedBackgroundTick":false,"isIsolatingForTestedPositiveBackgroundTick":false,"isIsolatingForTestedLFDPositiveBackgroundTick":false,"isIsolatingForTestedSelfRapidPositiveBackgroundTick":false,"hasHadRiskyContactBackgroundTick":false,"hasRiskyContactNotificationsEnabledBackgroundTick":false,"hasSelfDiagnosedBackgroundTick":false,"hasTestedPositiveBackgroundTick":false,"hasTestedLFDPositiveBackgroundTick":false,"hasTestedSelfRapidPositiveBackgroundTick":false,"encounterDetectionPausedBackgroundTick":false,"haveActiveIpcTokenBackgroundTick":false,"isIsolatingForUnconfirmedTestBackgroundTick":false,"hasReceivedRiskyVenueM2WarningBackgroundTick":false}"""
        private const val ANALYTICS_LOG_ENTRIES_JSON =
            """[{"instant":"2020-11-18T13:29:56.385Z","logItem":{"type":"BackgroundTaskCompletion","backgroundTaskTicks":$BACKGROUND_TASK_TICKS}},{"instant":"2020-11-18T13:30:15.120Z","logItem":{"type":"Event","eventType":"QR_CODE_CHECK_IN"}},{"instant":"2020-11-18T13:31:32.527Z","logItem":{"type":"ResultReceived","result":"POSITIVE","testKitType":"LAB_RESULT","testOrderType":"OUTSIDE_APP"}},{"instant":"2020-11-18T13:33:32.123Z","logItem":{"type":"UpdateNetworkStats","downloadedBytes":25,"uploadedBytes":15}},{"instant":"2020-11-18T13:33:33.123Z","logItem":{"type":"ExposureWindowMatched","totalRiskyExposures":1,"totalNonRiskyExposures":2}}]"""
        private const val RESULT_RECEIVED_NO_TEST_KIT_TYPE_JSON =
            """[{"instant":"2020-11-18T13:31:32.527Z","logItem":{"type":"ResultReceived","result":"POSITIVE","testOrderType":"OUTSIDE_APP"}}]"""
    }
}
