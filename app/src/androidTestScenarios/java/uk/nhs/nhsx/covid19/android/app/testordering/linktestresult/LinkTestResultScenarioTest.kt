package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot

class LinkTestResultScenarioTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val testResultRobot = TestResultRobot()

    @Test
    fun userEntersCtaTokenForPositiveTestResult_navigateToTestResultScreen() = reporter(
        scenario = "Enter test result",
        title = "Positive test result",
        description = "The user enters a CTA token and receives a positive test result",
        kind = FLOW
    ) {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        step(
            stepName = "Start",
            stepDescription = "User is presented a screen where they can enter a CTA token"
        )

        linkTestResultRobot.enterCtaToken("pstv-pstv")

        step(
            stepName = "Enter token",
            stepDescription = "User enters a valid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPositiveAndSelfIsolate() }

        step(
            stepName = "Positive test result",
            stepDescription = "User is informed that their test result is positive"
        )
    }

    @Test
    fun userEntersCtaTokenForNegativeTestResult_navigateToTestResultScreen() = reporter(
        scenario = "Enter test result",
        title = "Negative test result",
        description = "The user enters a CTA token and receives a negative test result",
        kind = FLOW
    ) {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken("f3dz-cfdt")

        step(
            stepName = "Enter token",
            stepDescription = "User enters a valid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysNegativeAndAlreadyFinishedIsolation() }

        step(
            stepName = "Negative test result",
            stepDescription = "User is informed that their test result is negative"
        )
    }

    @Test
    fun userEntersCtaTokenFoVoidTestResult_navigateToTestResultScreen() = reporter(
        scenario = "Enter test result",
        title = "Void test result",
        description = "The user enters a CTA token and receives a void test result",
        kind = FLOW
    ) {

        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken("8vb7-xehg")

        step(
            stepName = "Enter token",
            stepDescription = "User enters a valid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysVoidAndAlreadyFinishedIsolation() }

        step(
            stepName = "Void test result",
            stepDescription = "User is informed that their test result is void"
        )
    }

    @Test
    fun userEntersTooShortCtaToken_showErrorMessage() = notReported {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken("test")

        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkErrorIsDisplayed()
    }

    @Test
    fun userEntersInvalidCtaToken_showErrorMessage() = reporter(
        scenario = "Enter test result",
        title = "Invalid code",
        description = "An error message is shown after the user enters an invalid CTA token",
        kind = SCREEN
    ) {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken("aaaa-1337")

        step(
            stepName = "Enter token",
            stepDescription = "User enters an invalid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkErrorIsDisplayed()

        step(
            stepName = "Invalid code",
            stepDescription = "An error message is displayed to the user"
        )
    }

    private fun enterLinkTestResultFromStatusActivity() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()
    }
}
