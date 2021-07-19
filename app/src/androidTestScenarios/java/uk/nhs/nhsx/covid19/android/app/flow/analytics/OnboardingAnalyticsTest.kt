package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.CompleteOnboarding
import uk.nhs.nhsx.covid19.android.app.onboarding.WelcomeActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import kotlin.test.assertTrue

class OnboardingAnalyticsTest : AnalyticsTest() {

    private val completeOnboarding = CompleteOnboarding()

    @Before
    override fun setUp() {
        super.setUp()
        startTestActivity<WelcomeActivity>()
    }

    @Test
    fun completeOnboardingMetricsPresent() {
        completeOnboarding.onboard()

        assertOnLastFields {
            assertPresent(Metrics::completedOnboarding)
        }

        val lastRequest = testAppContext.analyticsApi.lastRequest().getOrAwaitValue()
        assertTrue(lastRequest.analyticsWindow.startDate == lastRequest.analyticsWindow.endDate)
    }
}
