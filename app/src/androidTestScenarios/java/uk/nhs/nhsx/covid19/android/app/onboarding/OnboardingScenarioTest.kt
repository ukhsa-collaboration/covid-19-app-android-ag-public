package uk.nhs.nhsx.covid19.android.app.onboarding

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AuthCodeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DataAndPrivacyRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MainOnboardingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PermissionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PostCodeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class OnboardingScenarioTest : EspressoTest() {

    private val mainOnBoardingRobot = MainOnboardingRobot()
    private val postCodeRobot = PostCodeRobot()
    private val permissionRobot = PermissionRobot()
    private val statusRobot = StatusRobot()
    private val dataAndPrivacyRobot = DataAndPrivacyRobot()
    private val authCodeRobot = AuthCodeRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun onboardingSuccessfulWithAuthenticationCodeSet_navigateToStatusScreen() = reporter(
        "Onboarding",
        "Happy path",
        "Enter a valid postcode and complete onboarding.",
        Reporter.Kind.FLOW
    ) {
        testAppContext.setExposureNotificationsEnabled(false, canBeChanged = false)
        testAppContext.setPostCode(null)
        testAppContext.setAuthenticated(true)

        startTestActivity<MainActivity>()

        mainOnBoardingRobot.checkActivityIsDisplayed()

        step(
            "Start",
            "The user is presented a screen with information on what " +
                "this app can do.\nThe user continues."
        )

        mainOnBoardingRobot.clickConfirmOnboarding()

        dataAndPrivacyRobot.checkActivityIsDisplayed()

        step(
            "Data and privacy",
            "The user is presented a screen with information on " +
                "data and privacy notes.\nThe user continues."
        )

        dataAndPrivacyRobot.clickConfirmOnboarding()

        permissionRobot.checkActivityIsDisplayed()

        step(
            "Permissions",
            "The user is presented with information on which permissions are " +
                "necessary for the app.\nThe user continues."
        )

        testAppContext.setExposureNotificationsEnabled(false, canBeChanged = true)
        permissionRobot.clickEnablePermissions()

        postCodeRobot.checkActivityIsDisplayed()

        step(
            "Enter postcode",
            "The user is asked to enter their partial postcode before " +
                "they can proceed."
        )

        postCodeRobot.enterPostCode("SE1")

        step(
            "Enter postcode – filled",
            "The user enters a valid postcode and continues."
        )

        postCodeRobot.clickContinue()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onboardingWithAuthenticationCodeNotSetSuccessful_navigateToStatusScreen() = reporter(
        "Onboarding",
        "Happy path",
        "Enter a valid postcode and complete onboarding.",
        Reporter.Kind.FLOW
    ) {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.ONBOARDING_AUTHENTICATION)

        testAppContext.setExposureNotificationsEnabled(false)
        testAppContext.setPostCode(null)
        testAppContext.setAuthenticated(false)

        startTestActivity<MainActivity>()

        authCodeRobot.checkActivityIsDisplayed()

        step(
            "Authentication code entry",
            "The user is asked to enter their partial authcode before" +
                "they can proceed."
        )

        authCodeRobot.enterAuthCode()

        authCodeRobot.clickContinue()

        mainOnBoardingRobot.checkActivityIsDisplayed()

        step(
            "Start",
            "The user is presented a screen with information on what " +
                "this app can do.\nThe user continues."
        )

        mainOnBoardingRobot.clickConfirmOnboarding()

        dataAndPrivacyRobot.checkActivityIsDisplayed()

        step(
            "Data and privacy",
            "The user is presented a screen with information on " +
                "data and privacy notes.\nThe user continues."
        )

        dataAndPrivacyRobot.clickConfirmOnboarding()

        permissionRobot.checkActivityIsDisplayed()

        step(
            "Permissions",
            "The user is presented with information on which permissions are " +
                "necessary for the app.\nThe user continues."
        )

        permissionRobot.clickEnablePermissions()

        postCodeRobot.checkActivityIsDisplayed()

        step(
            "Enter postcode",
            "The user is asked to enter their partial postcode before " +
                "they can proceed."
        )

        postCodeRobot.enterPostCode("SE1")

        step(
            "Enter postcode – filled",
            "The user enters a valid postcode and continues."
        )

        postCodeRobot.clickContinue()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onboardingFailedBecauseInvalidPostcodeEntered_showInvalidPostcodeError() = reporter(
        "Onboarding",
        "Unhappy path",
        "Enter invalid postcode",
        Reporter.Kind.FLOW
    ) {
        testAppContext.setExposureNotificationsEnabled(false, canBeChanged = false)
        testAppContext.setPostCode(null)
        testAppContext.setAuthenticated(true)

        startTestActivity<MainActivity>()

        mainOnBoardingRobot.checkActivityIsDisplayed()

        step(
            "Start",
            "The user is presented a screen with information on what this app " +
                "can do.\nThe user continues."
        )

        mainOnBoardingRobot.clickConfirmOnboarding()

        dataAndPrivacyRobot.checkActivityIsDisplayed()

        step(
            "Data and privacy",
            "The user is presented a screen with information on " +
                "data and privacy notes.\nThe user continues."
        )

        dataAndPrivacyRobot.clickConfirmOnboarding()

        permissionRobot.checkActivityIsDisplayed()

        step(
            "Permissions",
            "The user is presented with information on which permissions are " +
                "necessary for the app.\nThe user continues."
        )

        permissionRobot.clickEnablePermissions()

        postCodeRobot.checkActivityIsDisplayed()

        step(
            "Enter postcode",
            "The user is asked to enter their partial postcode before they " +
                "can proceed."
        )

        postCodeRobot.enterPostCode("INV")

        step(
            "Enter postcode – filled",
            "The user enters an invalid postcode and continues."
        )

        postCodeRobot.clickContinue()

        postCodeRobot.checkErrorContainerIsDisplayed()

        step(
            "Enter postcode – error",
            "The user is shown an error."
        )
    }

    @Test
    fun validPostcodeEnteredAndContinueClicked_navigateToPermissionScreen() = reporter(
        "Enter postcode",
        "Happy path",
        "Enter valid postcode",
        Reporter.Kind.SCREEN
    ) {
        testAppContext.setPostCode(null)

        startTestActivity<PostCodeActivity>()

        postCodeRobot.checkActivityIsDisplayed()

        step(
            "Start",
            "The user is asked to enter their postcode."
        )

        postCodeRobot.enterPostCode("ZE1")

        step(
            "Enter postcode",
            "After entering the postcode, the user can continue."
        )

        postCodeRobot.clickContinue()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun grantExposureNotificationPermissions_shouldEventuallyShowStatusScreen() = reporter(
        "Permissions",
        "Happy path",
        "Present permission request information",
        Reporter.Kind.SCREEN
    ) {
        testAppContext.setExposureNotificationsEnabled(false)
        testAppContext.setPostCode(null)

        startTestActivity<PermissionActivity>()

        permissionRobot.checkActivityIsDisplayed()

        step(
            "Start",
            "The user is informed about permissions need by the app."
        )

        permissionRobot.clickEnablePermissions()

        step(
            "Continued",
            "The user has tapped continued. In a live version of the app they " +
                "would see the permission alert dialogs."
        )
    }
}
