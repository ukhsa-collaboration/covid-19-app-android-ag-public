package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DailyContactTestingConfirmationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class DailyContactTesting() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val dailyContactTestingConfirmationRobot = DailyContactTestingConfirmationRobot()

    fun optIn() {
        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickLinkTestResult()
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
