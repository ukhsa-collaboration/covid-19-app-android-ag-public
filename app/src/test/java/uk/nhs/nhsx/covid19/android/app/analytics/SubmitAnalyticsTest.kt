package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Instant

class SubmitAnalyticsTest {
    private val analyticsEventsStorage = mockk<AnalyticsEventsStorage>()
    private val analyticsApi = mockk<AnalyticsApi>(relaxed = true)

    private val testSubject = SubmitAnalytics(analyticsEventsStorage, analyticsApi)

    @Test
    fun `successfully submit analytics events`() = runBlocking {
        every { analyticsEventsStorage.value } returns stubAnalyticsPayload(2)

        testSubject.invoke()

        coVerify(exactly = 2) { analyticsApi.submitAnalytics(any()) }

        verify { analyticsEventsStorage.value = null }
    }

    @Test
    fun `on submission error will clear events`() = runBlocking {
        every { analyticsEventsStorage.value } returns stubAnalyticsPayload(3)

        coEvery { analyticsApi.submitAnalytics(any()) } throws Exception()

        testSubject.invoke()

        verify { analyticsEventsStorage.value = null }
    }

    private fun stubAnalyticsPayload(size: Int) = mutableListOf<AnalyticsPayload>().apply {
        repeat(size) {
            add(
                AnalyticsPayload(
                    analyticsWindow = AnalyticsWindow(
                        Instant.now().toISOSecondsFormat(),
                        Instant.now().toISOSecondsFormat()
                    ),
                    includesMultipleApplicationVersions = false,
                    metadata = uk.nhs.nhsx.covid19.android.app.remote.data.Metadata(
                        deviceModel = "",
                        latestApplicationVersion = "",
                        operatingSystemVersion = "",
                        postalDistrict = ""
                    ),
                    metrics = Metrics()
                )
            )
        }
    }
}
