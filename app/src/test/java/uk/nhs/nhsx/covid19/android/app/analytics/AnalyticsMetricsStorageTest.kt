package uk.nhs.nhsx.covid19.android.app.analytics

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import kotlin.test.assertEquals

class AnalyticsMetricsStorageTest {

    private val moshi = Moshi.Builder().build()

    private val analyticsMetricsJsonStorage =
        mockk<AnalyticsMetricsJsonStorage>(relaxed = true)

    private val testSubject = AnalyticsMetricsStorage(
        analyticsMetricsJsonStorage,
        moshi
    )

    @Test
    fun `verify reset`() {
        every { analyticsMetricsJsonStorage.value } returns metricsJson

        testSubject.reset()

        verify { analyticsMetricsJsonStorage.value = resetMetricsJson }
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
        runningNormallyBackgroundTick = 15,
        totalBackgroundTasks = 16
    )

    private val metricsJson =
        """{"canceledCheckIn":1,"checkedIn":2,"completedOnboarding":3,"completedQuestionnaireAndStartedIsolation":4,"completedQuestionnaireButDidNotStartIsolation":5,"cumulativeDownloadBytes":6,"cumulativeUploadBytes":7,"encounterDetectionPausedBackgroundTick":8,"hasHadRiskyContactBackgroundTick":9,"hasSelfDiagnosedPositiveBackgroundTick":10,"isIsolatingBackgroundTick":11,"receivedNegativeTestResult":12,"receivedPositiveTestResult":13,"receivedVoidTestResult":14,"runningNormallyBackgroundTick":15,"totalBackgroundTasks":16}"""

    private val resetMetricsJson =
        """{"canceledCheckIn":0,"checkedIn":0,"completedOnboarding":0,"completedQuestionnaireAndStartedIsolation":0,"completedQuestionnaireButDidNotStartIsolation":0,"cumulativeDownloadBytes":6,"cumulativeUploadBytes":7,"encounterDetectionPausedBackgroundTick":0,"hasHadRiskyContactBackgroundTick":0,"hasSelfDiagnosedPositiveBackgroundTick":0,"isIsolatingBackgroundTick":0,"receivedNegativeTestResult":0,"receivedPositiveTestResult":0,"receivedVoidTestResult":0,"runningNormallyBackgroundTick":0,"totalBackgroundTasks":0}"""
}
