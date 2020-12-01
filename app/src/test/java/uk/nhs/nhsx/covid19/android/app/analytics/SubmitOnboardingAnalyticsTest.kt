package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.AnalyticsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.AnalyticsWindow
import uk.nhs.nhsx.covid19.android.app.util.toISOSecondsFormat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SubmitOnboardingAnalyticsTest {

    private val analyticsApi = mockk<AnalyticsApi>(relaxed = true)
    private val metadataProvider = mockk<MetadataProvider>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-09-28T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = SubmitOnboardingAnalytics(
        analyticsApi,
        metadataProvider,
        fixedClock
    )

    @Test
    fun `successfully submit onboarding analytics events`() = runBlocking {
        testSubject.invoke()

        val slot = slot<AnalyticsPayload>()
        coVerify(exactly = 1) { analyticsApi.submitAnalytics(capture(slot)) }

        val startOfToday = Instant.parse("2020-09-28T00:00:00Z").toISOSecondsFormat()
        val expectedAnalyticsWindow = AnalyticsWindow(startOfToday, startOfToday)
        assertEquals(expectedAnalyticsWindow, slot.captured.analyticsWindow)
        assertEquals(1, slot.captured.metrics.completedOnboarding)
    }
}
