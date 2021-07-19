package uk.nhs.nhsx.covid19.android.app.onboarding

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.ContactTracingHubAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BatteryOptimizationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ContactTracingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PolicyUpdateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PostCodeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class MainActivityFlowTest : EspressoTest() {

    private val policyUpdateRobot = PolicyUpdateRobot()
    private val localAuthorityInformationRobot = LocalAuthorityInformationRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val statusRobot = StatusRobot()
    private val batteryOptimizationRobot = BatteryOptimizationRobot()
    private val postCodeRobot = PostCodeRobot()
    private val contactTracingHubRobot = ContactTracingHubRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyNotAccepted_shouldShowPolicyUpdateScreen() {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(false)

        startTestActivity<MainActivity>()

        waitFor { policyUpdateRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAccepted_shouldShowLocalAuthorityInformationScreen() {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority(null)

        startTestActivity<MainActivity>()

        waitFor { localAuthorityInformationRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAcceptedWithBatteryOptimizationRequired_shouldBatteryOptimizationScreen() {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority("1")

        startTestActivity<MainActivity>()

        waitFor { batteryOptimizationRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAcceptedWithLocalAuthoritySet_shouldShowStatusScreen() {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority("1")

        startTestActivity<MainActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAcceptedWithLocalAuthorityMappingMissing_shouldCompleteFlow() {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setPostCode("BE22")

        startTestActivity<MainActivity>()

        waitFor { postCodeRobot.checkActivityIsDisplayed() }

        postCodeRobot.enterPostCode("N12")

        postCodeRobot.clickContinue()

        localAuthorityRobot.checkActivityIsDisplayed()

        waitFor { localAuthorityRobot.checkSingleAuthorityIsDisplayed("N12", "Barnet") }

        localAuthorityRobot.clickConfirm()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onPolicyUpdateScreen_userClicksContinueAndSeesStatusScreen() {
        startTestActivity<PolicyUpdateActivity>()

        policyUpdateRobot.clickContinue()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onExposureNotificationReminderAction_navigateToContactTracingHub() {
        setupOnboardingComplete()
        testAppContext.getExposureNotificationApi().setEnabled(false)

        startTestActivity<MainActivity> {
            putExtra(NotificationProvider.CONTACT_TRACING_HUB_ACTION, ContactTracingHubAction.ONLY_NAVIGATE)
        }

        waitFor { contactTracingHubRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun onExposureNotificationReminderAction_navigateToContactTracingHubAndTurnOnContactTracing() {
        setupOnboardingComplete()
        testAppContext.getExposureNotificationApi().setEnabled(false)
        testAppContext.getExposureNotificationApi().activationResult = Result.Success()

        startTestActivity<MainActivity> {
            putExtra(NotificationProvider.CONTACT_TRACING_HUB_ACTION, ContactTracingHubAction.NAVIGATE_AND_TURN_ON)
        }

        waitFor { contactTracingHubRobot.checkActivityIsDisplayed() }
        waitFor { contactTracingHubRobot.checkContactTracingToggledOnIsDisplayed() }
    }

    @Test
    fun whenNotificationFlagIsNotSet_navigateToStatusActivity() {
        setupOnboardingComplete()

        startTestActivity<MainActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    private fun setupOnboardingComplete() {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority("1")
    }
}
