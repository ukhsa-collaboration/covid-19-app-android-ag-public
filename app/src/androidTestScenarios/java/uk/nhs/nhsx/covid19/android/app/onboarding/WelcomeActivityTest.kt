package uk.nhs.nhsx.covid19.android.app.onboarding

import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.WelcomeRobot

class WelcomeActivityTest : EspressoTest() {

    private val welcomeRobot = WelcomeRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun onboardingNotFinishedAndAppStarted_shouldDisplayOnboardingWelcomeScreen() = notReported {
        testAppContext.setPostCode(null)

        startTestActivity<WelcomeActivity>()

        welcomeRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onboardingOnFirstStart_navigatesToWelcomeScreen() = notReported {
        testAppContext.setExposureNotificationsEnabled(false)
        testAppContext.setPostCode(null)

        startTestActivity<MainActivity>()

        welcomeRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onWelcomeActivity_userClicksConfirmAndSeesAgeConfirmationDialog() = notReported {
        startTestActivity<WelcomeActivity>()

        welcomeRobot.checkActivityIsDisplayed()

        welcomeRobot.clickConfirmOnboarding()

        welcomeRobot.checkAgeConfirmationDialogIsDisplayed()
    }
}
