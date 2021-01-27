package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
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
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsAlarm
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import java.time.Instant

class MigrateMetricsLogStorageToLogStorageTest {

    private val analyticsMetricsLogStorage = mockk<AnalyticsMetricsLogStorage>(relaxed = true)
    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxed = true)
    private val analyticsAlarm = mockk<AnalyticsAlarm>(relaxed = true)

    private val testSubject = MigrateMetricsLogStorageToLogStorage(
        analyticsMetricsLogStorage,
        analyticsLogStorage,
        analyticsAlarm
    )

    @Test
    fun `no migration needed if metrics log storage is empty`() = runBlocking {
        every { analyticsMetricsLogStorage.value } returns listOf()

        testSubject.invoke()

        verify(exactly = 0) { analyticsLogStorage.add(any()) }
        verify {
            analyticsMetricsLogStorage setProperty "value" value eq(listOf<MetricsLogEntry>())
            analyticsAlarm.cancel()
        }
    }

    @Test
    fun `successfully migrate metrics to log entries if metrics log storage is not empty`() = runBlocking {
        every { analyticsMetricsLogStorage.value } returns metricsLogStorageContent

        testSubject.invoke()

        verify(exactly = 1) {
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(CANCELED_CHECK_IN)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(QR_CODE_CHECK_IN)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION)))
            analyticsLogStorage.add(
                AnalyticsLogEntry(instant, Event(COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION))
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(instant, UpdateNetworkStats(downloadedBytes = 25, uploadedBytes = 15))
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(encounterDetectionPausedBackgroundTick = true))
                )
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(hasHadRiskyContactBackgroundTick = true))
                )
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(hasSelfDiagnosedPositiveBackgroundTick = true))
                )
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(isIsolatingBackgroundTick = true))
                )
            )
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(POSITIVE_RESULT_RECEIVED)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(NEGATIVE_RESULT_RECEIVED)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, Event(VOID_RESULT_RECEIVED)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(NEGATIVE, LAB_RESULT, INSIDE_APP)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(NEGATIVE, LAB_RESULT, OUTSIDE_APP)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(POSITIVE, LAB_RESULT, INSIDE_APP)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(POSITIVE, LAB_RESULT, OUTSIDE_APP)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(VOID, LAB_RESULT, INSIDE_APP)))
            analyticsLogStorage.add(AnalyticsLogEntry(instant, ResultReceived(VOID, LAB_RESULT, OUTSIDE_APP)))
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(runningNormallyBackgroundTick = true))
                )
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(hasTestedPositiveBackgroundTick = true))
                )
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(isIsolatingForSelfDiagnosedBackgroundTick = true))
                )
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(isIsolatingForTestedPositiveBackgroundTick = true))
                )
            )
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant,
                    BackgroundTaskCompletion(BackgroundTaskTicks(isIsolatingForHadRiskyContactBackgroundTick = true))
                )
            )
            analyticsMetricsLogStorage setProperty "value" value eq(listOf<MetricsLogEntry>())
            analyticsAlarm.cancel()
        }
    }

    companion object {
        private val instant = Instant.parse("2020-11-11T10:00:00Z")
        private val logEntry = MetricsLogEntry(Metrics(), instant)
        private val backgroundTaskMetrics = Metrics(totalBackgroundTasks = 1)

        private val canceledCheckIn = logEntry.copy(metrics = Metrics(canceledCheckIn = 1))
        private val checkedIn = logEntry.copy(metrics = Metrics(checkedIn = 1))
        private val completedOnboarding = logEntry.copy(metrics = Metrics(completedOnboarding = 1))
        private val completedQuestionnaireAndStartedIsolation =
            logEntry.copy(metrics = Metrics(completedQuestionnaireAndStartedIsolation = 1))
        private val completedQuestionnaireButDidNotStartIsolation =
            logEntry.copy(metrics = Metrics(completedQuestionnaireButDidNotStartIsolation = 1))
        private val networkStats =
            logEntry.copy(metrics = Metrics(cumulativeDownloadBytes = 25, cumulativeUploadBytes = 15))
        private val encounterDetectionPausedBackgroundTick =
            MetricsLogEntry(backgroundTaskMetrics.copy(encounterDetectionPausedBackgroundTick = 1), instant)
        private val hasHadRiskyContactBackgroundTick =
            logEntry.copy(backgroundTaskMetrics.copy(hasHadRiskyContactBackgroundTick = 1), instant)
        private val hasSelfDiagnosedPositiveBackgroundTick =
            MetricsLogEntry(backgroundTaskMetrics.copy(hasSelfDiagnosedPositiveBackgroundTick = 1), instant)
        private val isIsolatingBackgroundTick =
            MetricsLogEntry(backgroundTaskMetrics.copy(isIsolatingBackgroundTick = 1), instant)
        private val receivedNegativeTestResult = logEntry.copy(metrics = Metrics(receivedNegativeTestResult = 1))
        private val receivedPositiveTestResult = logEntry.copy(metrics = Metrics(receivedPositiveTestResult = 1))
        private val receivedVoidTestResult = logEntry.copy(metrics = Metrics(receivedVoidTestResult = 1))
        private val receivedNegativeTestResultEnteredManually =
            logEntry.copy(metrics = Metrics(receivedNegativeTestResultEnteredManually = 1))
        private val receivedNegativeTestResultViaPolling =
            logEntry.copy(metrics = Metrics(receivedNegativeTestResultViaPolling = 1))
        private val receivedPositiveTestResultEnteredManually =
            logEntry.copy(metrics = Metrics(receivedPositiveTestResultEnteredManually = 1))
        private val receivedPositiveTestResultViaPolling =
            logEntry.copy(metrics = Metrics(receivedPositiveTestResultViaPolling = 1))
        private val receivedVoidTestResultEnteredManually =
            logEntry.copy(metrics = Metrics(receivedVoidTestResultEnteredManually = 1))
        private val receivedVoidTestResultViaPolling =
            logEntry.copy(metrics = Metrics(receivedVoidTestResultViaPolling = 1))
        private val runningNormallyBackgroundTick =
            MetricsLogEntry(backgroundTaskMetrics.copy(runningNormallyBackgroundTick = 1), instant)
        private val totalBackgroundTasks = MetricsLogEntry(backgroundTaskMetrics, instant)
        private val hasTestedPositiveBackgroundTick =
            MetricsLogEntry(backgroundTaskMetrics.copy(hasTestedPositiveBackgroundTick = 1), instant)
        private val isIsolatingForSelfDiagnosedBackgroundTick =
            MetricsLogEntry(backgroundTaskMetrics.copy(isIsolatingForSelfDiagnosedBackgroundTick = 1), instant)
        private val isIsolatingForTestedPositiveBackgroundTick =
            MetricsLogEntry(backgroundTaskMetrics.copy(isIsolatingForTestedPositiveBackgroundTick = 1), instant)
        private val isIsolatingForHadRiskyContactBackgroundTick =
            MetricsLogEntry(backgroundTaskMetrics.copy(isIsolatingForHadRiskyContactBackgroundTick = 1), instant)

        private val metricsLogStorageContent: List<MetricsLogEntry> = listOf(
            canceledCheckIn,
            checkedIn,
            completedOnboarding,
            completedQuestionnaireAndStartedIsolation,
            completedQuestionnaireButDidNotStartIsolation,
            networkStats,
            encounterDetectionPausedBackgroundTick,
            hasHadRiskyContactBackgroundTick,
            hasSelfDiagnosedPositiveBackgroundTick,
            isIsolatingBackgroundTick,
            receivedNegativeTestResult,
            receivedPositiveTestResult,
            receivedVoidTestResult,
            receivedNegativeTestResultEnteredManually,
            receivedNegativeTestResultViaPolling,
            receivedPositiveTestResultEnteredManually,
            receivedPositiveTestResultViaPolling,
            receivedVoidTestResultEnteredManually,
            receivedVoidTestResultViaPolling,
            runningNormallyBackgroundTick,
            totalBackgroundTasks,
            hasTestedPositiveBackgroundTick,
            isIsolatingForSelfDiagnosedBackgroundTick,
            isIsolatingForTestedPositiveBackgroundTick,
            isIsolatingForHadRiskyContactBackgroundTick
        )
    }
}
