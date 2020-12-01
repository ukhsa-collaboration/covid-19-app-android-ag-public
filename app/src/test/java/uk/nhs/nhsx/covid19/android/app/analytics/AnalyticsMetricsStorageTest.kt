package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsMetricsJsonStorage
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsMetricsStorage

import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnalyticsMetricsStorageTest {

    private val moshi = Moshi.Builder().build()

    private val analyticsMetricsJsonStorage =
        mockk<AnalyticsMetricsJsonStorage>(relaxed = true)

    private val testSubject =
        AnalyticsMetricsStorage(
            analyticsMetricsJsonStorage,
            moshi
        )

    @Test
    fun `verify empty storage`() {
        every { analyticsMetricsJsonStorage.value } returns null

        val parsedMetrics = testSubject.metrics

        assertNull(parsedMetrics)
    }

    @Test
    fun `verify corrupted storage`() {
        every { analyticsMetricsJsonStorage.value } returns "dsfdsfsdfdsfdsf"

        val parsedMetrics = testSubject.metrics

        assertNull(parsedMetrics)
    }

    @Test
    fun `verify storing null`() {

        testSubject.metrics = null

        verify { analyticsMetricsJsonStorage.value = null }
    }

    @Test
    fun `verify serialization`() {
        every { analyticsMetricsJsonStorage.value } returns metricsJson

        val parsedMetrics = testSubject.metrics

        assertEquals(metrics, parsedMetrics)
    }

    @Test
    fun `verify deserialization`() {

        testSubject.metrics = metrics

        verify { analyticsMetricsJsonStorage.value = metricsJson }
    }

    private val metrics = Metrics(
        canceledCheckIn = 1,
        checkedIn = 2,
        completedOnboarding = 3,
        completedQuestionnaireAndStartedIsolation = 4,
        completedQuestionnaireButDidNotStartIsolation = 5,
        cumulativeDownloadBytes = 6,
        cumulativeUploadBytes = 7,
        encounterDetectionPausedBackgroundTick = 8,
        hasHadRiskyContactBackgroundTick = 9,
        hasSelfDiagnosedPositiveBackgroundTick = 10,
        isIsolatingBackgroundTick = 11,
        receivedNegativeTestResult = 12,
        receivedPositiveTestResult = 13,
        receivedVoidTestResult = 14,
        receivedNegativeTestResultEnteredManually = 15,
        receivedNegativeTestResultViaPolling = 16,
        receivedPositiveTestResultEnteredManually = 17,
        receivedPositiveTestResultViaPolling = 18,
        receivedVoidTestResultEnteredManually = 19,
        receivedVoidTestResultViaPolling = 20,
        runningNormallyBackgroundTick = 21,
        totalBackgroundTasks = 22,
        hasSelfDiagnosedBackgroundTick = 23,
        hasTestedPositiveBackgroundTick = 24,
        isIsolatingForSelfDiagnosedBackgroundTick = 25,
        isIsolatingForTestedPositiveBackgroundTick = 26,
        isIsolatingForHadRiskyContactBackgroundTick = 27
    )

    private val metricsJson =
        """{"canceledCheckIn":1,"checkedIn":2,"completedOnboarding":3,"completedQuestionnaireAndStartedIsolation":4,"completedQuestionnaireButDidNotStartIsolation":5,"cumulativeDownloadBytes":6,"cumulativeUploadBytes":7,"encounterDetectionPausedBackgroundTick":8,"hasHadRiskyContactBackgroundTick":9,"hasSelfDiagnosedPositiveBackgroundTick":10,"isIsolatingBackgroundTick":11,"receivedNegativeTestResult":12,"receivedPositiveTestResult":13,"receivedVoidTestResult":14,"receivedVoidTestResultEnteredManually":19,"receivedPositiveTestResultEnteredManually":17,"receivedNegativeTestResultEnteredManually":15,"receivedVoidTestResultViaPolling":20,"receivedPositiveTestResultViaPolling":18,"receivedNegativeTestResultViaPolling":16,"runningNormallyBackgroundTick":21,"totalBackgroundTasks":22,"hasSelfDiagnosedBackgroundTick":23,"hasTestedPositiveBackgroundTick":24,"isIsolatingForSelfDiagnosedBackgroundTick":25,"isIsolatingForTestedPositiveBackgroundTick":26,"isIsolatingForHadRiskyContactBackgroundTick":27}"""
}
