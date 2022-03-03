package uk.nhs.nhsx.covid19.android.app.onboarding

import com.jeroenmols.featureflag.framework.FeatureFlag.VENUE_CHECK_IN_BUTTON
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.WelcomeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class WelcomeActivityTest : EspressoTest() {

    private val welcomeRobot = WelcomeRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun onboardingNotFinishedAndAppStarted_shouldDisplayOnboardingWelcomeScreen() {
        testAppContext.setPostCode(null)

        startTestActivity<WelcomeActivity>()

        welcomeRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onboardingOnFirstStart_navigatesToWelcomeScreen() {
        testAppContext.setExposureNotificationsEnabled(false)
        testAppContext.setPostCode(null)
        testAppContext.setOnboardingCompleted(false)

        startTestActivity<MainActivity>()

        waitFor { welcomeRobot.checkActivityIsDisplayed() }
    }

    @Test
    @RetryFlakyTest
    fun onWelcomeActivity_userClicksConfirmAndSeesAgeConfirmationDialog() {
        startTestActivity<WelcomeActivity>()

        welcomeRobot.checkActivityIsDisplayed()

        welcomeRobot.clickConfirmOnboarding()

        waitFor { welcomeRobot.checkAgeConfirmationDialogIsDisplayed() }

        setScreenOrientation(LANDSCAPE)

        waitFor { welcomeRobot.checkAgeConfirmationDialogIsDisplayed() }

        setScreenOrientation(PORTRAIT)

        waitFor { welcomeRobot.checkAgeConfirmationDialogIsDisplayed() }
    }

    @Test
    fun whenVenueCheckInFeatureFlagIsDisabled_shouldDisplayVenueCheckInGroup() = runWithFeatureEnabled(VENUE_CHECK_IN_BUTTON) {
        startTestActivity<WelcomeActivity>()

        welcomeRobot.checkActivityIsDisplayed()

        welcomeRobot.checkVenueCheckInGroupIsDisplayed()
    }

    @Test
    fun whenVenueCheckInFeatureFlagIsDisabled_shouldNotDisplayVenueCheckInGroup() = runWithFeatureEnabled(VENUE_CHECK_IN_BUTTON, clearFeatureFlags = false) {
        startTestActivity<WelcomeActivity>()

        welcomeRobot.checkActivityIsDisplayed()

        welcomeRobot.checkVenueCheckInGroupIsDisplayed()
    }
}
