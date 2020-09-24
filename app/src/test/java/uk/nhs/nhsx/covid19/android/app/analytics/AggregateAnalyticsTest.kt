package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Instant

class AggregateAnalyticsTest {

    private val analyticsMetricsStorage = mockk<AnalyticsMetricsStorage>(relaxed = true)
    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val networkStatsStorage = mockk<NetworkTrafficStats>(relaxed = true)
    private val updateStatusStorage = mockk<UpdateStatusStorage>(relaxed = true)
    private val analyticsEventsStorage = mockk<AnalyticsEventsStorage>(relaxed = true)
    private val getNextAnalyticsWindow = mockk<GetAnalyticsWindow>()

    private val startWindowInstant = Instant.parse("2020-07-28T00:00:00.00Z")
    private val endWindowInstant = Instant.parse("2020-07-28T06:00:00.00Z")

    private val testSubject = AggregateAnalytics(
        analyticsMetricsStorage,
        postCodeProvider,
        networkStatsStorage,
        updateStatusStorage,
        analyticsEventsStorage,
        getNextAnalyticsWindow
    )

    @Before
    fun setUp() {
        every { getNextAnalyticsWindow.invoke() } returns (startWindowInstant to endWindowInstant)
        every { networkStatsStorage.getTotalBytesDownloaded() } returns null
        every { networkStatsStorage.getTotalBytesUploaded() } returns null
    }

    @Test
    fun `aggregating analytics add to existing storage`() = runBlocking {
        every { analyticsMetricsStorage.metrics } returns Metrics()
        every { analyticsEventsStorage.value } returns stubAnalyticsPayload(1)

        testSubject.invoke()

        verify(exactly = 1) { analyticsMetricsStorage.reset() }
        verify { analyticsEventsStorage.value = stubAnalyticsPayload(2) }
    }

    @Test
    fun `aggregating analytics add first event`() = runBlocking {
        every { analyticsMetricsStorage.metrics } returns Metrics()
        every { analyticsEventsStorage.value } returns emptyList()

        testSubject.invoke()

        verify(exactly = 1) { analyticsMetricsStorage.reset() }
        verify { analyticsEventsStorage.value = stubAnalyticsPayload(1) }
    }

    private fun stubAnalyticsPayload(size: Int) = mutableListOf<AnalyticsPayload>().apply {
        repeat(size) {
            add(
                AnalyticsPayload(
                    analyticsWindow = AnalyticsWindow(
                        startDate = startWindowInstant.toISOSecondsFormat(),
                        endDate = endWindowInstant.toISOSecondsFormat()
                    ),
                    includesMultipleApplicationVersions = false,
                    metadata = Metadata(
                        deviceModel = "null null",
                        latestApplicationVersion = if (BuildConfig.VERSION_NAME.contains(" ")) {
                            BuildConfig.VERSION_NAME.split(" ")[0]
                        } else {
                            BuildConfig.VERSION_NAME
                        },
                        operatingSystemVersion = "0",
                        postalDistrict = ""
                    ),
                    metrics = Metrics()
                )
            )
        }
    }
}
