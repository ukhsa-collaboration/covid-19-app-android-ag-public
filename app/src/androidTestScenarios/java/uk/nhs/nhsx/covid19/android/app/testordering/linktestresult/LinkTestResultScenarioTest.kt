package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NO_CONNECTION_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.PLOD_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.POSITIVE_RAPID_SELF_REPORTED_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.UNEXPECTED_ERROR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.UNKNOWN_RESULT_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_LFD_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.VOID_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.Contact
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UnknownTestResultRobot
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

@RunWith(Parameterized::class)
class LinkTestResultScenarioTest(override val configuration: TestConfiguration) : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val linkTestResultSymptomsRobot = LinkTestResultSymptomsRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val unknownTestResultRobot = UnknownTestResultRobot()

    @Before
    fun setUp() {
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
    }

    @Test
    @Reported
    fun userEntersCtaTokenForPcrPositiveTestResult_noSymptoms_navigateToTestResultScreen() = reporter(
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

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }

        step(
            stepName = "Symptoms",
            stepDescription = "User is presented a screen where they can confirm symptoms"
        )

        waitFor { linkTestResultSymptomsRobot.clickNo() }

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }

        step(
            stepName = "Positive test result",
            stepDescription = "User is informed that their test result is positive"
        )
    }

    @Test
    @Reported
    fun userEntersCtaTokenForPcrNegativeTestResult_navigateToTestResultScreen() = reporter(
        scenario = "Enter test result",
        title = "Negative test result",
        description = "The user enters a CTA token and receives a negative test result",
        kind = FLOW
    ) {
        enterLinkTestResultFromStatusActivity()

        waitFor { linkTestResultRobot.checkActivityIsDisplayed() }

        linkTestResultRobot.enterCtaToken(NEGATIVE_PCR_TOKEN)

        step(
            stepName = "Enter token",
            stepDescription = "User enters a valid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation() }

        step(
            stepName = "Negative test result",
            stepDescription = "User is informed that their test result is negative"
        )
    }

    @Test
    @Reported
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
    @Reported
    fun userEntersCtaTokenForPcrPlodTestResult_navigateToPlodTestResultScreen() = reporter(
        scenario = "Enter test result",
        title = "Plod test result",
        description = "The user enters a CTA token and receives a plod test result",
        kind = FLOW
    ) {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(PLOD_PCR_TOKEN)

        step(
            stepName = "Enter token",
            stepDescription = "User enters a valid token and taps 'Continue'"
        )

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPlodScreen() }

        step(
            stepName = "Plod test result",
            stepDescription = "User is informed that their test result is plod"
        )
    }

    @Test
    fun userIsContactCaseOnly_entersCtaTokenForPcrPositiveTestResult_noSymptoms_navigateToTestResultScreen() {
        testAppContext.setState(contactCaseOnlyIsolation)

        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        linkTestResultSymptomsRobot.clickNo()

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }
    }

    @Test
    fun userEntersCtaTokenForAssistedLfdPositiveTestResult_navigateToTestResultScreen() {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(POSITIVE_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
    }

    @Test
    fun userEntersCtaTokenForUnassistedLfdPositiveTestResult_navigateToTestResultScreen() {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(POSITIVE_RAPID_SELF_REPORTED_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
    }

    @Test
    fun userEntersCtaTokenForUnknownTestResult_navigatesToUnknownTestResultScreen() {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(UNKNOWN_RESULT_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { unknownTestResultRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun userEntersCtaTokenForLfdNegativeTestResult_showErrorMessage() {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(NEGATIVE_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkValidationErrorUnexpectedIsDisplayed()
    }

    @Test
    fun userEntersCtaTokenForLfdVoidTestResult_showErrorMessage() {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.enterCtaToken(VOID_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkValidationErrorUnexpectedIsDisplayed()
    }

    @Test
    fun userEntersTooShortCtaToken_showErrorMessage() {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken("test")

        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkValidationErrorInvalidTokenIsDisplayed()
    }

    @Test
    @Reported
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

        linkTestResultRobot.checkValidationErrorInvalidTokenIsDisplayed()

        step(
            stepName = "Invalid code",
            stepDescription = "An error message is displayed to the user"
        )
    }

    @Test
    fun noConnection_showErrorMessage() {
        startTestActivity<LinkTestResultActivity>()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(NO_CONNECTION_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkValidationErrorNoConnectionIsDisplayed() }
    }

    @Test
    fun unexpectedError_showErrorMessage() {
        startTestActivity<LinkTestResultActivity>()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(UNEXPECTED_ERROR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkValidationErrorUnexpectedIsDisplayed() }
    }

    private fun enterLinkTestResultFromStatusActivity() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()
    }

    private val contactCaseOnlyIsolation = IsolationState(
        isolationConfiguration = IsolationConfiguration(),
        contact = Contact(
            exposureDate = LocalDate.now().minus(2, DAYS),
            notificationDate = LocalDate.now().minus(2, DAYS)
        )
    )
}
