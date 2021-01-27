package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter

class AnalyticsMetricsLogStorageTest {

    private val analyticsMetricsLogJsonStorage = mockk<AnalyticsMetricsLogJsonStorage>(relaxed = true)
    private val moshi = Moshi.Builder().add(InstantAdapter()).build()

    private val fixedClock = Clock.fixed(Instant.parse("2020-07-27T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = AnalyticsMetricsLogStorage(
        analyticsMetricsLogJsonStorage,
        moshi
    )

    @Test
    fun `verify empty`() {
        every { analyticsMetricsLogJsonStorage.value } returns null

        val actual = testSubject.value

        assertEquals(emptyList(), actual)
    }

    @Test
    fun `verify corrupted storage`() {
        every { analyticsMetricsLogJsonStorage.value } returns "sdgsahdgashdjgd"

        val actual = testSubject.value

        assertEquals(emptyList(), actual)
    }

    @Test
    fun `verify serialization`() {
        every { analyticsMetricsLogJsonStorage.value } returns twoAnalyticsMetricsLogJson

        val actual = testSubject.value

        assertEquals(analyticsMetricsLog, actual)
    }

    @Test
    fun `verify deserialization`() {

        testSubject.value = analyticsMetricsLog

        verify { analyticsMetricsLogJsonStorage.value = twoAnalyticsMetricsLogJson }
    }

    @Test
    fun `verify add`() {
        every { analyticsMetricsLogJsonStorage.value } returns twoAnalyticsMetricsLogJson

        testSubject.add(
            MetricsLogEntry(
                Metrics(
                    cumulativeDownloadBytes = 1,
                    cumulativeUploadBytes = 1
                ),
                Instant.now(fixedClock)
            )
        )

        verify { analyticsMetricsLogJsonStorage.value = threeAnalyticsMetricsLogJson }
    }

    @Test
    fun `verify remove`() {
        every { analyticsMetricsLogJsonStorage.value } returns threeAnalyticsMetricsLogJson

        testSubject.remove(
            startInclusive = Instant.parse("2020-07-27T10:00:00Z"),
            endExclusive = Instant.parse("2020-07-28T00:00:00Z")
        )

        verify { analyticsMetricsLogJsonStorage.value = twoAnalyticsMetricsLogJson }
    }

    private val analyticsMetricsLog = listOf(
        MetricsLogEntry(
            Metrics(),
            Instant.parse("2020-07-26T10:00:00Z")
        ),
        MetricsLogEntry(
            Metrics(),
            Instant.parse("2020-07-28T00:00:00Z")
        )
    )

    private val twoAnalyticsMetricsLogJson =
        """
        [
        {"metrics":{"canceledCheckIn":0,"checkedIn":0,"completedOnboarding":0,"completedQuestionnaireAndStartedIsolation":0,"completedQuestionnaireButDidNotStartIsolation":0,"encounterDetectionPausedBackgroundTick":0,"hasHadRiskyContactBackgroundTick":0,"hasSelfDiagnosedPositiveBackgroundTick":0,"isIsolatingBackgroundTick":0,"receivedNegativeTestResult":0,"receivedPositiveTestResult":0,"receivedVoidTestResult":0,"receivedVoidTestResultEnteredManually":0,"receivedPositiveTestResultEnteredManually":0,"receivedNegativeTestResultEnteredManually":0,"receivedVoidTestResultViaPolling":0,"receivedPositiveTestResultViaPolling":0,"receivedNegativeTestResultViaPolling":0,"receivedVoidLFDTestResultEnteredManually":0,"receivedPositiveLFDTestResultEnteredManually":0,"receivedNegativeLFDTestResultEnteredManually":0,"receivedVoidLFDTestResultViaPolling":0,"receivedPositiveLFDTestResultViaPolling":0,"receivedNegativeLFDTestResultViaPolling":0,"runningNormallyBackgroundTick":0,"totalBackgroundTasks":0,"hasSelfDiagnosedBackgroundTick":0,"hasTestedPositiveBackgroundTick":0,"hasTestedLFDPositiveBackgroundTick":0,"isIsolatingForSelfDiagnosedBackgroundTick":0,"isIsolatingForTestedPositiveBackgroundTick":0,"isIsolatingForTestedLFDPositiveBackgroundTick":0,"isIsolatingForHadRiskyContactBackgroundTick":0,"receivedRiskyContactNotification":0,"startedIsolation":0,"receivedActiveIpcToken":0,"haveActiveIpcTokenBackgroundTick":0,"selectedIsolationPaymentsButton":0,"launchedIsolationPaymentsApplication":0,"totalExposureWindowsNotConsideredRisky":0,"totalExposureWindowsConsideredRisky":0},"instant":"2020-07-26T10:00:00Z"},
        {"metrics":{"canceledCheckIn":0,"checkedIn":0,"completedOnboarding":0,"completedQuestionnaireAndStartedIsolation":0,"completedQuestionnaireButDidNotStartIsolation":0,"encounterDetectionPausedBackgroundTick":0,"hasHadRiskyContactBackgroundTick":0,"hasSelfDiagnosedPositiveBackgroundTick":0,"isIsolatingBackgroundTick":0,"receivedNegativeTestResult":0,"receivedPositiveTestResult":0,"receivedVoidTestResult":0,"receivedVoidTestResultEnteredManually":0,"receivedPositiveTestResultEnteredManually":0,"receivedNegativeTestResultEnteredManually":0,"receivedVoidTestResultViaPolling":0,"receivedPositiveTestResultViaPolling":0,"receivedNegativeTestResultViaPolling":0,"receivedVoidLFDTestResultEnteredManually":0,"receivedPositiveLFDTestResultEnteredManually":0,"receivedNegativeLFDTestResultEnteredManually":0,"receivedVoidLFDTestResultViaPolling":0,"receivedPositiveLFDTestResultViaPolling":0,"receivedNegativeLFDTestResultViaPolling":0,"runningNormallyBackgroundTick":0,"totalBackgroundTasks":0,"hasSelfDiagnosedBackgroundTick":0,"hasTestedPositiveBackgroundTick":0,"hasTestedLFDPositiveBackgroundTick":0,"isIsolatingForSelfDiagnosedBackgroundTick":0,"isIsolatingForTestedPositiveBackgroundTick":0,"isIsolatingForTestedLFDPositiveBackgroundTick":0,"isIsolatingForHadRiskyContactBackgroundTick":0,"receivedRiskyContactNotification":0,"startedIsolation":0,"receivedActiveIpcToken":0,"haveActiveIpcTokenBackgroundTick":0,"selectedIsolationPaymentsButton":0,"launchedIsolationPaymentsApplication":0,"totalExposureWindowsNotConsideredRisky":0,"totalExposureWindowsConsideredRisky":0},"instant":"2020-07-28T00:00:00Z"}
        ]
        """.trimIndent().replace("\n", "")

    private val threeAnalyticsMetricsLogJson =
        """
        [
        {"metrics":{"canceledCheckIn":0,"checkedIn":0,"completedOnboarding":0,"completedQuestionnaireAndStartedIsolation":0,"completedQuestionnaireButDidNotStartIsolation":0,"encounterDetectionPausedBackgroundTick":0,"hasHadRiskyContactBackgroundTick":0,"hasSelfDiagnosedPositiveBackgroundTick":0,"isIsolatingBackgroundTick":0,"receivedNegativeTestResult":0,"receivedPositiveTestResult":0,"receivedVoidTestResult":0,"receivedVoidTestResultEnteredManually":0,"receivedPositiveTestResultEnteredManually":0,"receivedNegativeTestResultEnteredManually":0,"receivedVoidTestResultViaPolling":0,"receivedPositiveTestResultViaPolling":0,"receivedNegativeTestResultViaPolling":0,"receivedVoidLFDTestResultEnteredManually":0,"receivedPositiveLFDTestResultEnteredManually":0,"receivedNegativeLFDTestResultEnteredManually":0,"receivedVoidLFDTestResultViaPolling":0,"receivedPositiveLFDTestResultViaPolling":0,"receivedNegativeLFDTestResultViaPolling":0,"runningNormallyBackgroundTick":0,"totalBackgroundTasks":0,"hasSelfDiagnosedBackgroundTick":0,"hasTestedPositiveBackgroundTick":0,"hasTestedLFDPositiveBackgroundTick":0,"isIsolatingForSelfDiagnosedBackgroundTick":0,"isIsolatingForTestedPositiveBackgroundTick":0,"isIsolatingForTestedLFDPositiveBackgroundTick":0,"isIsolatingForHadRiskyContactBackgroundTick":0,"receivedRiskyContactNotification":0,"startedIsolation":0,"receivedActiveIpcToken":0,"haveActiveIpcTokenBackgroundTick":0,"selectedIsolationPaymentsButton":0,"launchedIsolationPaymentsApplication":0,"totalExposureWindowsNotConsideredRisky":0,"totalExposureWindowsConsideredRisky":0},"instant":"2020-07-26T10:00:00Z"},
        {"metrics":{"canceledCheckIn":0,"checkedIn":0,"completedOnboarding":0,"completedQuestionnaireAndStartedIsolation":0,"completedQuestionnaireButDidNotStartIsolation":0,"encounterDetectionPausedBackgroundTick":0,"hasHadRiskyContactBackgroundTick":0,"hasSelfDiagnosedPositiveBackgroundTick":0,"isIsolatingBackgroundTick":0,"receivedNegativeTestResult":0,"receivedPositiveTestResult":0,"receivedVoidTestResult":0,"receivedVoidTestResultEnteredManually":0,"receivedPositiveTestResultEnteredManually":0,"receivedNegativeTestResultEnteredManually":0,"receivedVoidTestResultViaPolling":0,"receivedPositiveTestResultViaPolling":0,"receivedNegativeTestResultViaPolling":0,"receivedVoidLFDTestResultEnteredManually":0,"receivedPositiveLFDTestResultEnteredManually":0,"receivedNegativeLFDTestResultEnteredManually":0,"receivedVoidLFDTestResultViaPolling":0,"receivedPositiveLFDTestResultViaPolling":0,"receivedNegativeLFDTestResultViaPolling":0,"runningNormallyBackgroundTick":0,"totalBackgroundTasks":0,"hasSelfDiagnosedBackgroundTick":0,"hasTestedPositiveBackgroundTick":0,"hasTestedLFDPositiveBackgroundTick":0,"isIsolatingForSelfDiagnosedBackgroundTick":0,"isIsolatingForTestedPositiveBackgroundTick":0,"isIsolatingForTestedLFDPositiveBackgroundTick":0,"isIsolatingForHadRiskyContactBackgroundTick":0,"receivedRiskyContactNotification":0,"startedIsolation":0,"receivedActiveIpcToken":0,"haveActiveIpcTokenBackgroundTick":0,"selectedIsolationPaymentsButton":0,"launchedIsolationPaymentsApplication":0,"totalExposureWindowsNotConsideredRisky":0,"totalExposureWindowsConsideredRisky":0},"instant":"2020-07-28T00:00:00Z"},
        {"metrics":{"canceledCheckIn":0,"checkedIn":0,"completedOnboarding":0,"completedQuestionnaireAndStartedIsolation":0,"completedQuestionnaireButDidNotStartIsolation":0,"cumulativeDownloadBytes":1,"cumulativeUploadBytes":1,"encounterDetectionPausedBackgroundTick":0,"hasHadRiskyContactBackgroundTick":0,"hasSelfDiagnosedPositiveBackgroundTick":0,"isIsolatingBackgroundTick":0,"receivedNegativeTestResult":0,"receivedPositiveTestResult":0,"receivedVoidTestResult":0,"receivedVoidTestResultEnteredManually":0,"receivedPositiveTestResultEnteredManually":0,"receivedNegativeTestResultEnteredManually":0,"receivedVoidTestResultViaPolling":0,"receivedPositiveTestResultViaPolling":0,"receivedNegativeTestResultViaPolling":0,"receivedVoidLFDTestResultEnteredManually":0,"receivedPositiveLFDTestResultEnteredManually":0,"receivedNegativeLFDTestResultEnteredManually":0,"receivedVoidLFDTestResultViaPolling":0,"receivedPositiveLFDTestResultViaPolling":0,"receivedNegativeLFDTestResultViaPolling":0,"runningNormallyBackgroundTick":0,"totalBackgroundTasks":0,"hasSelfDiagnosedBackgroundTick":0,"hasTestedPositiveBackgroundTick":0,"hasTestedLFDPositiveBackgroundTick":0,"isIsolatingForSelfDiagnosedBackgroundTick":0,"isIsolatingForTestedPositiveBackgroundTick":0,"isIsolatingForTestedLFDPositiveBackgroundTick":0,"isIsolatingForHadRiskyContactBackgroundTick":0,"receivedRiskyContactNotification":0,"startedIsolation":0,"receivedActiveIpcToken":0,"haveActiveIpcTokenBackgroundTick":0,"selectedIsolationPaymentsButton":0,"launchedIsolationPaymentsApplication":0,"totalExposureWindowsNotConsideredRisky":0,"totalExposureWindowsConsideredRisky":0},"instant":"2020-07-27T10:00:00Z"}
        ]
        """.trimIndent().replace("\n", "")
}
