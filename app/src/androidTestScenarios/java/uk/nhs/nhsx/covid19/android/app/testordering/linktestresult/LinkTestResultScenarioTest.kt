package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NO_CONNECTION_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_RAPID_SELF_REPORTED_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.UNEXPECTED_ERROR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot

class LinkTestResultScenarioTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val testResultRobot = TestResultRobot()

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
    }

    @Test
    fun userEntersCtaTokenForPcrPositiveTestResult_navigateToTestResultScreen() = reporter(
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

        linkTestResultRobot.enterCtaToken(POSITIVE_PCR_TOKEN)

        step(
            stepName = "Enter token",
            stepDescription = "User enters a valid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        step(
            stepName = "Positive test result",
            stepDescription = "User is informed that their test result is positive"
        )
    }

    @Test
    fun userEntersCtaTokenForPcrNegativeTestResult_navigateToTestResultScreen() = reporter(
        scenario = "Enter test result",
        title = "Negative test result",
        description = "The user enters a CTA token and receives a negative test result",
        kind = FLOW
    ) {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(NEGATIVE_PCR_TOKEN)

        step(
            stepName = "Enter token",
            stepDescription = "User enters a valid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        step(
            stepName = "Negative test result",
            stepDescription = "User is informed that their test result is negative"
        )
    }

    @Test
    fun userEntersCtaTokenForPcrVoidTestResult_navigateToTestResultScreen() = reporter(
        scenario = "Enter test result",
        title = "Void test result",
        description = "The user enters a CTA token and receives a void test result",
        kind = FLOW
    ) {

        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(VOID_PCR_TOKEN)

        step(
            stepName = "Enter token",
            stepDescription = "User enters a valid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysVoidNotInIsolation() }

        step(
            stepName = "Void test result",
            stepDescription = "User is informed that their test result is void"
        )
    }

    @Test
    fun userEntersCtaTokenForAssistedLfdPositiveTestResult_navigateToTestResultScreen() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(POSITIVE_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
    }

    @Test
    fun userEntersCtaTokenForUnassistedLfdPositiveTestResult_navigateToTestResultScreen() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(POSITIVE_RAPID_SELF_REPORTED_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
    }

    @Test
    fun userEntersCtaTokenForLfdNegativeTestResult_showErrorMessage() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(NEGATIVE_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkErrorUnexpectedIsDisplayed()
    }

    @Test
    fun userEntersCtaTokenForLfdVoidTestResult_showErrorMessage() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(VOID_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkErrorUnexpectedIsDisplayed()
    }

    @Test
    fun userEntersTooShortCtaToken_showErrorMessage() = notReported {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken("test")

        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkErrorInvalidTokenIsDisplayed()
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

        linkTestResultRobot.checkErrorInvalidTokenIsDisplayed()

        step(
            stepName = "Invalid code",
            stepDescription = "An error message is displayed to the user"
        )
    }

    @Test
    fun noConnection_showErrorMessage() = notReported {
        startTestActivity<LinkTestResultActivity>()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(NO_CONNECTION_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkErrorNoConnectionIsDisplayed() }
    }

    @Test
    fun unexpectedError_showErrorMessage() = notReported {
        startTestActivity<LinkTestResultActivity>()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(UNEXPECTED_ERROR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkErrorUnexpectedIsDisplayed() }
    }

    private fun enterLinkTestResultFromStatusActivity() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()
    }
}
