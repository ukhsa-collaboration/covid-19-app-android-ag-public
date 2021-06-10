package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DailyContactTestingConfirmationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class DailyContactTesting {

    private val statusRobot = StatusRobot()
    private val testingHubRobot = TestingHubRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val dailyContactTestingConfirmationRobot = DailyContactTestingConfirmationRobot()

    fun optIn() {
        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickTestingHub()
        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickEnterTestResult()
        linkTestResultRobot.checkActivityIsDisplayed()
        waitFor { linkTestResultRobot.checkDailyContactTestingContainerIsDisplayed() }
        linkTestResultRobot.selectDailyContactTestingOptIn()
        linkTestResultRobot.clickContinue()
        dailyContactTestingConfirmationRobot.checkActivityIsDisplayed()
        dailyContactTestingConfirmationRobot.clickConfirmOptInToOpenDialog()
        waitFor { dailyContactTestingConfirmationRobot.checkDailyContactTestingOptInConfirmationDialogIsDisplayed() }
        dailyContactTestingConfirmationRobot.clickDialogConfirmOptIn()
        statusRobot.checkActivityIsDisplayed()
    }
}
