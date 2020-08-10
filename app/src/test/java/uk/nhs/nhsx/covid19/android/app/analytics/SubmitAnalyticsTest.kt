package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.lang.Exception
import java.time.Instant

class SubmitAnalyticsTest {

    private val analyticsMetricsStorage = mockk<AnalyticsMetricsStorage>(relaxed = true)
    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val networkStatsStorage = mockk<NetworkTrafficStats>(relaxed = true)
    private val updateStatusStorage = mockk<UpdateStatusStorage>(relaxed = true)
    private val analyticsLastSubmittedDate = mockk<AnalyticsLastSubmittedDate>(relaxed = true)
    private val analyticsApi = mockk<AnalyticsApi>(relaxed = true)

    private val testSubject = SubmitAnalytics(
        analyticsMetricsStorage,
        postCodeProvider,
        networkStatsStorage,
        updateStatusStorage,
        analyticsLastSubmittedDate,
        analyticsApi
    )

    @Test
    fun `submitting analytics updates storage and last submitted time`() = runBlocking {
        every { analyticsMetricsStorage.metrics } returns Metrics()
        every { analyticsLastSubmittedDate.lastSubmittedDate } returns Instant.now().toISOSecondsFormat()

        testSubject.invoke()

        verify(exactly = 1) { analyticsMetricsStorage.reset() }
        verify(exactly = 1) { analyticsLastSubmittedDate.lastSubmittedDate = any() }
    }

    @Test
    fun `when submitting analytics throws error storage is still updated`() = runBlocking {
        every { analyticsMetricsStorage.metrics } returns Metrics()
        coEvery { analyticsApi.submitAnalytics(any()) } throws Exception()
        every { analyticsLastSubmittedDate.lastSubmittedDate } returns Instant.now().toISOSecondsFormat()

        testSubject.invoke()

        verify { analyticsMetricsStorage.reset() }
        verify { analyticsLastSubmittedDate.lastSubmittedDate = any() }
    }

    @Test
    fun `when submitting analytics last submitted value is no set yet storage is not updated`() = runBlocking {
        every { analyticsMetricsStorage.metrics } returns Metrics()
        every { analyticsLastSubmittedDate.lastSubmittedDate } returns null

        testSubject.invoke()

        verify(exactly = 0) { analyticsMetricsStorage.reset() }
        verify(exactly = 1) { analyticsLastSubmittedDate.lastSubmittedDate = any() }
    }
}
