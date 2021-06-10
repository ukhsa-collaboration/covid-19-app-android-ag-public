package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import com.jeroenmols.featureflag.framework.FeatureFlag.DAILY_CONTACT_TESTING
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
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
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DailyContactTestingConfirmationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UnknownTestResultRobot
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

class LinkTestResultScenarioTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val linkTestResultRobot = LinkTestResultRobot()
    private val linkTestResultSymptomsRobot = LinkTestResultSymptomsRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val unknownTestResultRobot = UnknownTestResultRobot()
    private val dailyContactTestingConfirmationRobot = DailyContactTestingConfirmationRobot()
    private val isolationChecker = IsolationChecker(testAppContext)

    @Before
    fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(DAILY_CONTACT_TESTING)
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
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

        linkTestResultSymptomsRobot.clickNo()

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

        waitFor { linkTestResultRobot.checkActivityIsDisplayed() }

        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()

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

        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()

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
    fun userEntersCtaTokenForPcrPlodTestResult_navigateToPlodTestResultScreen() = reporter(
        scenario = "Enter test result",
        title = "Plod test result",
        description = "The user enters a CTA token and receives a plod test result",
        kind = FLOW
    ) {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()

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
    fun userIsContactCaseOnly_entersCtaTokenForPcrPositiveTestResult_noSymptoms_navigateToTestResultScreen() = notReported {
        testAppContext.setState(contactCaseOnlyIsolation)

        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        waitFor { linkTestResultRobot.checkDailyContactTestingContainerIsDisplayed() }

        linkTestResultRobot.enterCtaToken(POSITIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultSymptomsRobot.checkActivityIsDisplayed() }

        linkTestResultSymptomsRobot.clickNo()

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation() }
    }

    @Test
    fun userEntersCtaTokenForAssistedLfdPositiveTestResult_navigateToTestResultScreen() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()
        linkTestResultRobot.enterCtaToken(POSITIVE_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
    }

    @Test
    fun userEntersCtaTokenForUnassistedLfdPositiveTestResult_navigateToTestResultScreen() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()
        linkTestResultRobot.enterCtaToken(POSITIVE_RAPID_SELF_REPORTED_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation() }
    }

    @Test
    fun userEntersCtaTokenForUnknownTestResult_navigatesToUnknownTestResultScreen() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()
        linkTestResultRobot.enterCtaToken(UNKNOWN_RESULT_TOKEN)
        linkTestResultRobot.clickContinue()

        waitFor { unknownTestResultRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun userEntersCtaTokenForLfdNegativeTestResult_showErrorMessage() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()
        linkTestResultRobot.enterCtaToken(NEGATIVE_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkValidationErrorUnexpectedIsDisplayed()
    }

    @Test
    fun userEntersCtaTokenForLfdVoidTestResult_showErrorMessage() = notReported {
        enterLinkTestResultFromStatusActivity()
        linkTestResultRobot.checkActivityIsDisplayed()
        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()
        linkTestResultRobot.enterCtaToken(VOID_LFD_TOKEN)
        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkValidationErrorUnexpectedIsDisplayed()
    }

    @Test
    fun userEntersTooShortCtaToken_showErrorMessage() = notReported {
        enterLinkTestResultFromStatusActivity()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()

        linkTestResultRobot.enterCtaToken("test")

        linkTestResultRobot.clickContinue()

        linkTestResultRobot.checkValidationErrorInvalidTokenIsDisplayed()
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

        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()

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
    fun noConnection_showErrorMessage() = notReported {
        startTestActivity<LinkTestResultActivity>()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()

        linkTestResultRobot.enterCtaToken(NO_CONNECTION_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkValidationErrorNoConnectionIsDisplayed() }
    }

    @Test
    fun unexpectedError_showErrorMessage() = notReported {
        startTestActivity<LinkTestResultActivity>()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.checkDailyContactTestingContainerIsNotDisplayed()

        linkTestResultRobot.enterCtaToken(UNEXPECTED_ERROR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkValidationErrorUnexpectedIsDisplayed() }
    }

    @Test
    fun userInContactCaseOnly_inLinkTestResultScreen_checkOptInToDailyContactTesting_confirmOptIn_transitionOutOfIsolation() =
        reporter(
            scenario = "Daily contact testing",
            title = "Opt-in",
            description = "User is in contact case only isolation and opts-in to daily contact testing.",
            kind = FLOW
        ) {
            testAppContext.setState(contactCaseOnlyIsolation)

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            step(
                stepName = "Contact case only isolation",
                stepDescription = "The user is in contact case only isolation."
            )

            statusRobot.clickLinkTestResult()

            linkTestResultRobot.checkActivityIsDisplayed()

            waitFor { linkTestResultRobot.checkDailyContactTestingContainerIsDisplayed() }

            linkTestResultRobot.selectDailyContactTestingOptIn()

            step(
                stepName = "Daily contact testing opt-in",
                stepDescription = "The user navigates to the manual test code entry screen, selects to opt-in to daily contact testing and taps the 'Continue' button."
            )

            linkTestResultRobot.clickContinue()

            dailyContactTestingConfirmationRobot.checkActivityIsDisplayed()

            step(
                stepName = "Daily contact testing confirmation",
                stepDescription = "The user is presented with information about opting in to daily contact testing."
            )

            dailyContactTestingConfirmationRobot.clickConfirmOptInToOpenDialog()

            waitFor { dailyContactTestingConfirmationRobot.checkDailyContactTestingOptInConfirmationDialogIsDisplayed() }

            step(
                stepName = "Confirmation dialog",
                stepDescription = "The user is presented a confirmation dialog to opt-in to daily contact testing and proceeds with pressing 'Confirm'."
            )

            dailyContactTestingConfirmationRobot.clickDialogConfirmOptIn()

            statusRobot.checkActivityIsDisplayed()

            step(
                stepName = "Opt-in complete",
                stepDescription = "The user has opted in to daily contact testing and is released from isolation."
            )

            isolationChecker.assertExpiredContactNoIndex()
        }

    private fun enterLinkTestResultFromStatusActivity() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickLinkTestResult()
    }

    private val contactCaseOnlyIsolation = IsolationState(
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            exposureDate = LocalDate.now().minus(2, DAYS),
            notificationDate = LocalDate.now().minus(2, DAYS),
            expiryDate = LocalDate.now().plusDays(12)
        )
    )
}
