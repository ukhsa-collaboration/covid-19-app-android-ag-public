package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.CompleteOnboarding
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import kotlin.test.assertEquals

class OnboardingAnalyticsTest : AnalyticsTest() {

    private val completeOnboarding = CompleteOnboarding()

    @Test
    fun completeOnboardingMetricsPresent() {
        testAppContext.setOnboardingCompleted(false)

        startTestActivity<MainActivity>()
        completeOnboarding.onboard()

        assertOnLastFields {
            assertPresent(Metrics::completedOnboarding)
        }

        val lastRequest = testAppContext.analyticsApi.lastRequest().getOrAwaitValue()
        assertEquals(lastRequest.analyticsWindow.startDate, lastRequest.analyticsWindow.endDate)
    }
}
