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
    fun riskLevelNeutralLowInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Neutral risk (low)",
        description = "User enters home screen and realizes they are in a Neutral (low) risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("AL1", "Neutral")
    }

    @Test
    fun riskLevelNeutralLowBasedOnLocalAuthority_startInStatusActivity_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Neutral risk (low) based on local authority",
        description = "User enters home screen and realizes they are in a Neutral (low) risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)
        riskLevelFlow("AL1", "Neutral", localAuthorityId = "E07000240")
    }

    @Test
    fun riskLevelGreenLowInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Green risk (low)",
        description = "User enters home screen and realizes they are in a Green (low) risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("AL2", "Green")
    }

    @Test
    fun riskLevelAmberMediumInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Amber risk (medium)",
        description = "User enters home screen and realizes they are in a Amber (medium) risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("AL4", "Amber")
    }

    @Test
    fun riskLevelYellowMediumInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Yellow risk (medium)",
        description = "User enters home screen and realizes they are in a Yellow (medium) risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("CM2", "Yellow")
    }

    @Test
    fun riskLevelRedHighInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Red risk (high)",
        description = "User enters home screen and realizes they are in a Red (high) risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        riskLevelFlow("CM1", "Red")
    }

    @Test
    fun riskLevelTierFourBasedOnLocalAuthority_startInStatusActivity_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Tier 4 risk (high) based on local authority",
        description = "User enters home screen and realizes they are in a Tier 4 (high) risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)
        riskLevelFlow("SE1", "maroon", localAuthorityId = "E09000022")
    }

    @Test
    fun riskLevelFiveBasedOnLocalAuthority_startInStatusActivity_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Tier 5 risk (very high) based on local authority",
        description = "User enters home screen and realizes they are in a Tier 5 (very high) risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)
        riskLevelFlow("SE2", "black", localAuthorityId = "E09000004")
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
