package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlin.test.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter

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
        every { analyticsLogEntryJsonStorage.value } returns analyticsLogEntriesJson

        val parsedAnalyticsLogEntries = testSubject.value

        assertEquals(expected = analyticsLogEntries, actual = parsedAnalyticsLogEntries)
    }

    @Test
    fun `verify deserialization`() {
        testSubject.value = analyticsLogEntries

        verify { analyticsLogEntryJsonStorage.value = analyticsLogEntriesJson }
    }

    @Test
    fun `verify adding new log entry`() {
        testSubject.value = analyticsLogEntries

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
        every { analyticsLogEntryJsonStorage.value } returns analyticsLogEntriesJson

        testSubject.remove(
            startInclusive = Instant.parse("2020-11-18T13:30:00.000Z"),
            endExclusive = Instant.parse("2020-11-18T13:32:00.000Z")
        )

        val updatedAnalyticsLogEntriesJson =
            """[{"instant":"2020-11-18T13:29:56.385Z","logItem":{"type":"BackgroundTaskCompletion","backgroundTaskTicks":{"runningNormallyBackgroundTick":true,"isIsolatingBackgroundTick":true,"isIsolatingForHadRiskyContactBackgroundTick":true,"hasSelfDiagnosedPositiveBackgroundTick":false,"isIsolatingForSelfDiagnosedBackgroundTick":false,"isIsolatingForTestedPositiveBackgroundTick":false,"hasHadRiskyContactBackgroundTick":false,"hasSelfDiagnosedBackgroundTick":false,"hasTestedPositiveBackgroundTick":false,"encounterDetectionPausedBackgroundTick":false,"haveActiveIpcTokenBackgroundTick":false}}},{"instant":"2020-11-18T13:33:32.123Z","logItem":{"type":"UpdateNetworkStats","downloadedBytes":25,"uploadedBytes":15}}]"""
        verify {
            analyticsLogEntryJsonStorage setProperty "value" value eq(updatedAnalyticsLogEntriesJson)
        }
    }

    @Test
    fun `verify no events are removed when none match the specified window`() {
        every { analyticsLogEntryJsonStorage.value } returns analyticsLogEntriesJson

        testSubject.remove(
            startInclusive = Instant.parse("2020-11-18T13:28:00.000Z"),
            endExclusive = Instant.parse("2020-11-18T13:29:00.000Z")
        )

        verify { analyticsLogEntryJsonStorage setProperty "value" value eq(analyticsLogEntriesJson) }
    }

    private val analyticsLogEntries = listOf(
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
            logItem = ResultReceived(result = POSITIVE, testOrderType = OUTSIDE_APP)
        ),
        AnalyticsLogEntry(
            instant = Instant.parse("2020-11-18T13:33:32.123Z"),
            logItem = UpdateNetworkStats(downloadedBytes = 25, uploadedBytes = 15)
        )
    )

    private val analyticsLogEntriesJson =
        """[{"instant":"2020-11-18T13:29:56.385Z","logItem":{"type":"BackgroundTaskCompletion","backgroundTaskTicks":{"runningNormallyBackgroundTick":true,"isIsolatingBackgroundTick":true,"isIsolatingForHadRiskyContactBackgroundTick":true,"hasSelfDiagnosedPositiveBackgroundTick":false,"isIsolatingForSelfDiagnosedBackgroundTick":false,"isIsolatingForTestedPositiveBackgroundTick":false,"hasHadRiskyContactBackgroundTick":false,"hasSelfDiagnosedBackgroundTick":false,"hasTestedPositiveBackgroundTick":false,"encounterDetectionPausedBackgroundTick":false,"haveActiveIpcTokenBackgroundTick":false}}},{"instant":"2020-11-18T13:30:15.120Z","logItem":{"type":"Event","eventType":"QR_CODE_CHECK_IN"}},{"instant":"2020-11-18T13:31:32.527Z","logItem":{"type":"ResultReceived","result":"POSITIVE","testOrderType":"OUTSIDE_APP"}},{"instant":"2020-11-18T13:33:32.123Z","logItem":{"type":"UpdateNetworkStats","downloadedBytes":25,"uploadedBytes":15}}]"""
}
