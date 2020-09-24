package uk.nhs.nhsx.covid19.android.app.status

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskLevelRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class RiskLevelFlowTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val riskLevelRobot = RiskLevelRobot()

    @Test
    fun riskLevelLowInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Low risk",
        description = "User enters home screen and realizes they are in a low risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        testAppContext.setPostCode("A2")

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkAreaRiskViewIsDisplayed() }

        step(
            stepName = "Home screen - low area risk",
            stepDescription = "Home screen shows that the area risk level for the user's post district code is low. User clicks on the area risk view to get more information."
        )

        statusRobot.clickAreaRiskView()

        riskLevelRobot.checkActivityIsDisplayed()

        step(
            stepName = "Risk level details",
            stepDescription = "User is presented with information about the meaning of a low risk level area."
        )
    }

    @Test
    fun riskLevelMediumInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "Medium risk",
        description = "User enters home screen and realizes they are in a medium risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        testAppContext.setPostCode("CM2")

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkAreaRiskViewIsDisplayed() }

        step(
            stepName = "Home screen - medium area risk",
            stepDescription = "Home screen shows that the area risk level for the user's post district code is medium. User clicks on the area risk view to get more information."
        )

        statusRobot.clickAreaRiskView()

        riskLevelRobot.checkActivityIsDisplayed()

        step(
            stepName = "Risk level details",
            stepDescription = "User is presented with information about the meaning of a medium risk level area."
        )
    }

    @Test
    fun riskLevelHighInStatusScreen_navigateToRiskLevelScreen() = reporter(
        scenario = "Area risk level",
        title = "High risk",
        description = "User enters home screen and realizes they are in a high risk area. They tap on it to get more information.",
        kind = FLOW
    ) {
        testAppContext.setPostCode("CM1")

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkAreaRiskViewIsDisplayed() }

        step(
            stepName = "Home screen - high area risk",
            stepDescription = "Home screen shows that the area risk level for the user's post district code is high. User clicks on the area risk view to get more information."
        )

        statusRobot.clickAreaRiskView()

        riskLevelRobot.checkActivityIsDisplayed()

        step(
            stepName = "Risk level details",
            stepDescription = "User is presented with information about the meaning of a high risk level area."
        )
    }
}
