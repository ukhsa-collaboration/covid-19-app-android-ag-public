package uk.nhs.nhsx.covid19.android.app.onboarding

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MainOnboardingRobot

class MainOnboardingActivityTest : EspressoTest() {

    private val mainOnBoardingRobot = MainOnboardingRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun onboardingNotFinishedAndAppStarted_shouldDisplayOnboardingWelcomeScreen() = notReported {
        testAppContext.setPostCode(null)

        startTestActivity<MainOnboardingActivity>()

        mainOnBoardingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onboardingWhenAuthenticated_navigatesToMainOnBoardingScreen() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.ONBOARDING_AUTHENTICATION)

        testAppContext.setExposureNotificationsEnabled(false, canBeChanged = false)
        testAppContext.setPostCode(null)
        testAppContext.setAuthenticated(true)

        startTestActivity<MainActivity>()

        mainOnBoardingRobot.checkActivityIsDisplayed()
    }
}
