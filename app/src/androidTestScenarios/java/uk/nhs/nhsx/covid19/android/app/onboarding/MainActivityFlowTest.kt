package uk.nhs.nhsx.covid19.android.app.onboarding

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PolicyUpdateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class MainActivityFlowTest : EspressoTest() {

    private val policyUpdateRobot = PolicyUpdateRobot()
    private val statusRobot = StatusRobot()

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyNotAccepted_shouldShowPolicyUpdateScreen() = notReported {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(false)

        startTestActivity<MainActivity>()

        policyUpdateRobot.checkActivityIsDisplayed()
    }

    @Test
    fun startingAppWithOnboardingCompletedAndPolicyAccepted_shouldShowStatusScreen() = notReported {
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)

        startTestActivity<MainActivity>()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onPolicyUpdateScreen_userClicksContinueAndSeesStatusScreen() = notReported {
        startTestActivity<PolicyUpdateActivity>()

        policyUpdateRobot.clickContinue()

        statusRobot.checkActivityIsDisplayed()
    }
}
