package uk.nhs.nhsx.covid19.android.app.onboarding

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BatteryOptimizationRobot
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

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyNotAccepted_shouldShowPolicyUpdateScreen() = notReported {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(false)

        startTestActivity<MainActivity>()

        waitFor { policyUpdateRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAcceptedWithLocalAuthorityFeatureFlagEnabled_shouldShowLocalAuthorityInformationScreen() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority(null)

        startTestActivity<MainActivity>()

        waitFor { localAuthorityInformationRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAcceptedWithLocalAuthorityFeatureFlagDisabled_shouldShowStatusScreen() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)

        startTestActivity<MainActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAcceptedWithLocalAuthorityFeatureFlagDisabledAndBatteryOptimizationRequired_shouldBatteryOptimizationScreen() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.BATTERY_OPTIMIZATION)

        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)

        startTestActivity<MainActivity>()

        waitFor { batteryOptimizationRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAcceptedWithLocalAuthoritySet_shouldShowStatusScreen() = notReported {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority("1")

        startTestActivity<MainActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAcceptedWithLocalAuthorityMappingMissing_shouldCompleteFlow() = notReported {
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
    fun onPolicyUpdateScreen_userClicksContinueAndSeesStatusScreen() = notReported {
        startTestActivity<PolicyUpdateActivity>()

        policyUpdateRobot.clickContinue()

        statusRobot.checkActivityIsDisplayed()
    }
}
