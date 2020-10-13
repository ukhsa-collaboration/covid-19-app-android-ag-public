package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metadata
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

class GroupAnalyticsEventsTest {

    private val analyticsMetricsLogStorage = mockk<AnalyticsMetricsLogStorage>(relaxed = true)
    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val updateStatusStorage = mockk<UpdateStatusStorage>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-09-28T00:05:00.00Z"), ZoneOffset.UTC)

    private val getAnalyticsWindow = GetAnalyticsWindow(fixedClock)

    private val testSubject = GroupAnalyticsEvents(
        analyticsMetricsLogStorage,
        postCodeProvider,
        updateStatusStorage,
        getAnalyticsWindow,
        fixedClock
    )

    @Test
    fun `failure on reading events log returns failure`() = runBlocking {
        val exception = Exception()

        every { analyticsMetricsLogStorage.value } throws exception

        val actual = testSubject.invoke(shallIncludeCurrentWindow = false)

        val expected = Failure(exception)

        assertEquals(expected, actual)
    }

    @Test
    fun `filter out the current window`() = runBlocking {
        every { analyticsMetricsLogStorage.value } returns currentWindowMetrics.plus(lastWindowMetrics)

        val actual = testSubject.invoke(shallIncludeCurrentWindow = false)

        val expected = Success(
            listOf(
                AnalyticsPayload(
                    analyticsWindow = AnalyticsWindow(
                        startDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat(),
                        endDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat()
                    ),
                    includesMultipleApplicationVersions = false,
                    metadata = stubMetadata(),
                    metrics = Metrics()
                )
            )
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `group events in two analytics windows excluding events from current window`() =
        runBlocking {
            every { analyticsMetricsLogStorage.value } returns currentWindowMetrics.plus(
                lastWindowMetrics
            ).plus(oldWindowMetrics)

            val actual = testSubject.invoke(shallIncludeCurrentWindow = false)

            val expected = Success(
                listOf(
                    AnalyticsPayload(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat()
                        ),
                        includesMultipleApplicationVersions = false,
                        metadata = stubMetadata(),
                        metrics = Metrics()
                    ),
                    AnalyticsPayload(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-26T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat()
                        ),
                        includesMultipleApplicationVersions = false,
                        metadata = stubMetadata(),
                        metrics = Metrics()
                    )
                )
            )

            assertEquals(expected, actual)
        }

    @Test
    fun `group events in two analytics windows including events from current window`() =
        runBlocking {
            every { analyticsMetricsLogStorage.value } returns currentWindowMetrics.plus(
                lastWindowMetrics
            ).plus(oldWindowMetrics)

            val actual = testSubject.invoke(shallIncludeCurrentWindow = true)

            val expected = Success(
                listOf(
                    AnalyticsPayload(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-29T00:00:00Z").toISOSecondsFormat()
                        ),
                        includesMultipleApplicationVersions = false,
                        metadata = stubMetadata(),
                        metrics = Metrics()
                    ),
                    AnalyticsPayload(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat()
                        ),
                        includesMultipleApplicationVersions = false,
                        metadata = stubMetadata(),
                        metrics = Metrics()
                    ),
                    AnalyticsPayload(
                        analyticsWindow = AnalyticsWindow(
                            startDate = Instant.parse("2020-09-26T00:00:00Z").toISOSecondsFormat(),
                            endDate = Instant.parse("2020-09-27T00:00:00Z").toISOSecondsFormat()
                        ),
                        includesMultipleApplicationVersions = false,
                        metadata = stubMetadata(),
                        metrics = Metrics()
                    )
                )
            )

            assertEquals(expected, actual)
        }

    private val currentWindowMetrics = listOf(
        MetricsLogEntry(
            Metrics(),
            Instant.parse("2020-09-28T00:00:00Z")
        ),
        MetricsLogEntry(
            Metrics(),
            Instant.parse("2020-09-28T23:59:59Z")
        )
    )

    private val lastWindowMetrics = MetricsLogEntry(
        Metrics(),
        Instant.parse("2020-09-27T00:00:00Z")
    )

    private val oldWindowMetrics = MetricsLogEntry(
        Metrics(),
        Instant.parse("2020-09-26T00:00:00Z")
    )

    private fun stubMetadata() = Metadata(
        deviceModel = "null null",
        latestApplicationVersion = if (BuildConfig.VERSION_NAME.contains(" ")) {
            BuildConfig.VERSION_NAME.split(" ")[0]
        } else {
            BuildConfig.VERSION_NAME
        },
        operatingSystemVersion = "0",
        postalDistrict = ""
    )
}
