package uk.nhs.nhsx.covid19.android.app.testordering.unknownresult

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UnknownTestResultRobot

class UnknownTestResultScenarioTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val unknownTestResultRobot = UnknownTestResultRobot()

    @Test
    fun startAppWithUnknownTestResult_showsUnknownTestResultScreen_closingScreenShowsStatusScreen() = notReported {
        testAppContext.getReceivedUnknownTestResultProvider().value = true

        startTestActivity<StatusActivity>()

        waitFor { unknownTestResultRobot.checkActivityIsDisplayed() }

        unknownTestResultRobot.clickCloseButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startAppWithUnknownTestResult_showsUnknownTestResultScreen_pressingBackShowsStatusScreen() = notReported {
        testAppContext.getReceivedUnknownTestResultProvider().value = true

        startTestActivity<StatusActivity>()

        waitFor { unknownTestResultRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
