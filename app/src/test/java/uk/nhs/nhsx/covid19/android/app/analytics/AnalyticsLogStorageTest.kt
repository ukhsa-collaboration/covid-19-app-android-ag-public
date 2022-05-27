package uk.nhs.nhsx.covid19.android.app.analytics

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ExposureWindowMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogStorage.Companion.VALUE_KEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import java.time.Instant

class AnalyticsLogStorageTest : ProviderTest<AnalyticsLogStorage, List<AnalyticsLogEntry>>() {

    override val getTestSubject = ::AnalyticsLogStorage
    override val property = AnalyticsLogStorage::value
    override val key = VALUE_KEY
    override val defaultValue: List<AnalyticsLogEntry> = listOf()
    override val expectations: List<ProviderTestExpectation<List<AnalyticsLogEntry>>> = listOf(
        ProviderTestExpectation(json = ANALYTICS_LOG_ENTRIES_JSON, objectValue = ANALYTICS_LOG_ENTRIES),
        ProviderTestExpectation(
            json = RESULT_RECEIVED_NO_TEST_KIT_TYPE_JSON,
            objectValue = MIGRATED_LOG_ENTRY,
            direction = JSON_TO_OBJECT
        )
    )

    @Test
    fun `verify adding new log entry`() {
        sharedPreferencesReturns("[]")

        val newLogEntry = AnalyticsLogEntry(
            instant = Instant.parse("2020-11-18T13:40:56.333Z"),
            logItem = Event(POSITIVE_RESULT_RECEIVED)
        )

        testSubject.add(newLogEntry)

        val newLogEntryJson =
            """[{"instant":"2020-11-18T13:40:56.333Z","logItem":{"type":"Event","eventType":"POSITIVE_RESULT_RECEIVED"}}]"""

        assertSharedPreferenceSetsValue(newLogEntryJson)
    }

    @Test
    fun `verify removal of analytics events that are within the given window`() {
        sharedPreferencesReturns(ANALYTICS_LOG_ENTRIES_JSON)

        testSubject.remove(
            startInclusive = Instant.parse("2020-11-18T13:29:00.000Z"),
            endExclusive = Instant.parse("2020-11-18T13:32:00.000Z")
        )

        val updatedAnalyticsLogEntriesJson =
            """[{"instant":"2020-11-18T13:33:32.123Z","logItem":{"type":"UpdateNetworkStats","downloadedBytes":25,"uploadedBytes":15}},{"instant":"2020-11-18T13:33:33.123Z","logItem":{"type":"ExposureWindowMatched","totalRiskyExposures":1,"totalNonRiskyExposures":2}}]"""
        assertSharedPreferenceSetsValue(updatedAnalyticsLogEntriesJson)
    }

    @Test
    fun `verify no events are removed when none match the specified window`() {
        sharedPreferencesReturns(ANALYTICS_LOG_ENTRIES_JSON)

        testSubject.remove(
            startInclusive = Instant.parse("2020-11-18T13:28:00.000Z"),
            endExclusive = Instant.parse("2020-11-18T13:29:00.000Z")
        )

        assertSharedPreferenceSetsValue(ANALYTICS_LOG_ENTRIES_JSON)
    }

    @Test
    fun `verify events before or equal to specified date are removed`() {
        sharedPreferencesReturns(ANALYTICS_LOG_ENTRIES_JSON)

        testSubject.removeBeforeOrEqual(Instant.parse("2020-11-18T13:31:32.527Z"))

        val updatedAnalyticsLogEntriesJson =
            """[{"instant":"2020-11-18T13:33:32.123Z","logItem":{"type":"UpdateNetworkStats","downloadedBytes":25,"uploadedBytes":15}},{"instant":"2020-11-18T13:33:33.123Z","logItem":{"type":"ExposureWindowMatched","totalRiskyExposures":1,"totalNonRiskyExposures":2}}]"""
        assertSharedPreferenceSetsValue(updatedAnalyticsLogEntriesJson)
    }

    @Test
    fun `verify no events are removed when none are before or equal to the specified date`() {
        sharedPreferencesReturns(ANALYTICS_LOG_ENTRIES_JSON)

        testSubject.removeBeforeOrEqual(Instant.parse("2020-11-18T13:29:56.123Z"))

        assertSharedPreferenceSetsValue(ANALYTICS_LOG_ENTRIES_JSON)
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

        private val MIGRATED_LOG_ENTRY = listOf(
            AnalyticsLogEntry(
                instant = Instant.parse("2020-11-18T13:31:32.527Z"),
                logItem = ResultReceived(
                    result = POSITIVE,
                    testKitType = LAB_RESULT,
                    testOrderType = OUTSIDE_APP
                )
            )
        )

        private const val BACKGROUND_TASK_TICKS =
            """{"runningNormallyBackgroundTick":true,"isIsolatingBackgroundTick":true,"isIsolatingForHadRiskyContactBackgroundTick":true,"isIsolatingForSelfDiagnosedBackgroundTick":false,"isIsolatingForTestedPositiveBackgroundTick":false,"isIsolatingForTestedLFDPositiveBackgroundTick":false,"isIsolatingForTestedSelfRapidPositiveBackgroundTick":false,"hasHadRiskyContactBackgroundTick":false,"hasRiskyContactNotificationsEnabledBackgroundTick":false,"hasSelfDiagnosedBackgroundTick":false,"hasTestedPositiveBackgroundTick":false,"hasTestedLFDPositiveBackgroundTick":false,"hasTestedSelfRapidPositiveBackgroundTick":false,"encounterDetectionPausedBackgroundTick":false,"haveActiveIpcTokenBackgroundTick":false,"isIsolatingForUnconfirmedTestBackgroundTick":false,"hasReceivedRiskyVenueM2WarningBackgroundTick":false,"isDisplayingLocalInfoBackgroundTick":false,"optedOutForContactIsolationBackgroundTick":false,"appIsUsableBackgroundTick":false,"appIsUsableBluetoothOffBackgroundTick":false,"appIsContactTraceableBackgroundTick":false,"hasCompletedV2SymptomsQuestionnaireBackgroundTick":false,"hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick":false}"""
        private const val ANALYTICS_LOG_ENTRIES_JSON =
            """[{"instant":"2020-11-18T13:29:56.385Z","logItem":{"type":"BackgroundTaskCompletion","backgroundTaskTicks":$BACKGROUND_TASK_TICKS}},{"instant":"2020-11-18T13:30:15.120Z","logItem":{"type":"Event","eventType":"QR_CODE_CHECK_IN"}},{"instant":"2020-11-18T13:31:32.527Z","logItem":{"type":"ResultReceived","result":"POSITIVE","testKitType":"LAB_RESULT","testOrderType":"OUTSIDE_APP"}},{"instant":"2020-11-18T13:33:32.123Z","logItem":{"type":"UpdateNetworkStats","downloadedBytes":25,"uploadedBytes":15}},{"instant":"2020-11-18T13:33:33.123Z","logItem":{"type":"ExposureWindowMatched","totalRiskyExposures":1,"totalNonRiskyExposures":2}}]"""
        private const val RESULT_RECEIVED_NO_TEST_KIT_TYPE_JSON =
            """[{"instant":"2020-11-18T13:31:32.527Z","logItem":{"type":"ResultReceived","result":"POSITIVE","testOrderType":"OUTSIDE_APP"}}]"""
    }
}
