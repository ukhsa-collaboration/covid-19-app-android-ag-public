package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AggregateAnalytics
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsEventsStorage
import uk.nhs.nhsx.covid19.android.app.analytics.legacy.AnalyticsMetricsStorage
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Deprecated("Use GroupAnalyticsEvents, this is only for migration")
class AggregateAnalyticsTest {

    private val analyticsMetricsStorage = mockk<AnalyticsMetricsStorage>(relaxed = true)
    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val networkStatsStorage = mockk<NetworkTrafficStats>(relaxed = true)
    private val updateStatusStorage = mockk<UpdateStatusStorage>(relaxed = true)
    private val analyticsEventsStorage = mockk<AnalyticsEventsStorage>(relaxed = true)

    private val fixedClock = Clock.fixed(Instant.parse("2020-09-28T00:05:00.00Z"), ZoneOffset.UTC)

    private val getAnalyticsWindow = GetAnalyticsWindow(fixedClock)

    private val testSubject =
        AggregateAnalytics(
            analyticsMetricsStorage,
            postCodeProvider,
            networkStatsStorage,
            updateStatusStorage,
            analyticsEventsStorage,
            getAnalyticsWindow
        )

    @Before
    fun setUp() {
        every { networkStatsStorage.getTotalBytesDownloaded() } returns null
        every { networkStatsStorage.getTotalBytesUploaded() } returns null
    }

    @Test
    fun `aggregating analytics add to existing storage`() = runBlocking {
        every { analyticsMetricsStorage.metrics } returns Metrics()
        every { analyticsEventsStorage.value } returns stubAnalyticsPayload(1)

        testSubject.invoke()

        verify(exactly = 1) { analyticsMetricsStorage.metrics = null }
        verify { analyticsEventsStorage.value = stubAnalyticsPayload(2) }
    }

    @Test
    fun `aggregating analytics add first event`() = runBlocking {
        every { analyticsMetricsStorage.metrics } returns Metrics()
        every { analyticsEventsStorage.value } returns emptyList()

        testSubject.invoke()

        verify(exactly = 1) { analyticsMetricsStorage.metrics = null }
        verify { analyticsEventsStorage.value = stubAnalyticsPayload(1) }
    }

    @Test
    fun `don't aggregate analytics on empty metrics storage`() = runBlocking {
        every { analyticsMetricsStorage.metrics } returns null

        testSubject.invoke()

        verify(exactly = 0) { analyticsMetricsStorage.metrics = null }
        verify(exactly = 0) { analyticsEventsStorage.value = any() }
    }

    private fun stubAnalyticsPayload(size: Int) = mutableListOf<AnalyticsPayload>().apply {
        repeat(size) {
            add(
                AnalyticsPayload(
                    analyticsWindow = AnalyticsWindow(
                        startDate = "2020-09-27T00:00:00Z",
                        endDate = "2020-09-28T00:00:00Z"
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
