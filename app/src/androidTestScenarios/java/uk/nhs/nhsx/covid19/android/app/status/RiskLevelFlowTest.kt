package uk.nhs.nhsx.covid19.android.app.status

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskLevelRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class RiskLevelFlowTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val riskLevelRobot = RiskLevelRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun riskLevelNeutralInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Neutral risk",
        description = "User enters home screen and realizes they are in a neutral risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("AL1", "neutral")
    }

    @Test
    fun riskLevelLowInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Low risk",
        description = "User enters home screen and realizes they are in a low risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("AL2", "low")
    }

    @Test
    fun riskLevelAmberInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Amber risk",
        description = "User enters home screen and realizes they are in a amber risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("AL4", "amber")
    }

    @Test
    fun riskLevelMediumInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Medium risk",
        description = "User enters home screen and realizes they are in a medium risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("CM2", "medium")
    }

    @Test
    fun riskLevelHighInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "High risk",
        description = "User enters home screen and realizes they are in a high risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("CM1", "high")
    }

    @Test
    fun riskLevelHighBasedOnLocalAuthority_startInStatusActivity_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level based on local authority",
        title = "Neutral risk",
        description = "User enters home screen and realizes they are in a neutral risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)
        riskLevelFlow("AL1", "neutral", localAuthorityId = "E07000240")
    }

    private fun Reporter.riskLevelFlow(postCode: String, riskLevel: String, localAuthorityId: String? = null) {
        testAppContext.setPostCode(postCode)

        if (localAuthorityId != null) {
            testAppContext.setLocalAuthority(localAuthorityId)
        }

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        // This is necessary because ExposureApplication does not invoke the download tasks when onboarding is not completed
        runBackgroundTasks()

        waitFor { statusRobot.checkAreaRiskViewIsDisplayed() }

        step(
            stepName = "Home screen - $riskLevel area risk",
            stepDescription = "Home screen shows that the area risk level for the user's post district code is $riskLevel. User clicks on the area risk view to get more information."
        )

        statusRobot.clickAreaRiskView()

        riskLevelRobot.checkActivityIsDisplayed()

        step(
            stepName = "Risk level details",
            stepDescription = "User is presented with information about the meaning of a $riskLevel risk level area."
        )
    }
}
