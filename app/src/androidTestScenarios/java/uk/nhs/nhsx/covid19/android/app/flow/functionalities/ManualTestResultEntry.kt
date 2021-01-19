package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class ManualTestResultEntry(private val testAppContext: TestApplicationContext) {
    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val testResultRobot = TestResultRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()

    fun enterPositive() {
        manuallyEnterTestResult(positiveResultToken)

        waitFor { testResultRobot.checkActivityDisplaysPositiveAndSelfIsolate() }

        testResultRobot.clickIsolationActionButton()

        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }

        shareKeysInformationRobot.clickIUnderstandButton()
    }

    fun enterNegative() {
        manuallyEnterTestResult(negativeResultToken)
    }

    fun enterVoid() {
        manuallyEnterTestResult(voidResultToken)
    }

    private fun manuallyEnterTestResult(token: String) {
        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.clickLinkTestResult()

        waitFor { linkTestResultRobot.checkActivityIsDisplayed() }

        testAppContext.virologyTestingApi.testEndDate = testAppContext.clock.instant()

        linkTestResultRobot.enterCtaToken(token)

        linkTestResultRobot.clickContinue()
    }

    companion object {
        private const val positiveResultToken = "pstv-pstv"
        private const val negativeResultToken = "f3dz-cfdt"
        private const val voidResultToken = "8vb7-xehg"
    }
}
