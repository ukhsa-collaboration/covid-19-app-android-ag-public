package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.test.platform.app.InstrumentationRegistry
import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import java.time.LocalDate

@RunWith(Parameterized::class)
class LinkTestResultActivityTest(override val configuration: TestConfiguration) : EspressoTest() {

    private val linkTestResultRobot = LinkTestResultRobot()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    @Reported
    fun userIsContactCaseOnly_providesNeitherCtaTokenNorDailyContactTestingOptIn_showErrorMessage() = reporter(
        scenario = "Enter test result",
        title = "Neither provided",
        description = "An error message is shown when the user does not enter a CTA token or check the checkbox",
        kind = SCREEN
    ) {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.DAILY_CONTACT_TESTING)

        testAppContext.setState(contactCaseOnlyIsolation)

        startTestActivity<LinkTestResultActivity>()

        linkTestResultRobot.checkActivityIsDisplayed()

        step(
            stepName = "Click continue",
            stepDescription = "User taps 'Continue' without entering CTA token or choosing to opt-in to daily contact testing"
        )

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkInputErrorNeitherProvidedIsDisplayed() }

        step(
            stepName = "Neither provided",
            stepDescription = "An error message is displayed to the user"
        )
    }

    @Test
    @Reported
    fun userIsContactCaseOnly_providesBothCtaTokenAndDailyContactTestingOptIn_showErrorMessage() = reporter(
        scenario = "Enter test result",
        title = "Both provided",
        description = "An error message is shown when the user provides both a CTA token and opt-in to daily contact testing",
        kind = SCREEN
    ) {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.DAILY_CONTACT_TESTING)

        testAppContext.setState(contactCaseOnlyIsolation)

        startTestActivity<LinkTestResultActivity>()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(MockVirologyTestingApi.POSITIVE_PCR_TOKEN)

        linkTestResultRobot.selectDailyContactTestingOptIn()

        step(
            stepName = "Click continue",
            stepDescription = "User taps 'Continue' while both providing a CTA token and choosing to opt-in to daily contact testing"
        )

        linkTestResultRobot.clickContinue()

        waitFor { linkTestResultRobot.checkInputErrorBothProvidedIsDisplayed() }

        step(
            stepName = "Both provided",
            stepDescription = "An error message is displayed to the user"
        )
    }

    @Test
    fun userTapsOnLink_NavigateToExternalLink() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            startTestActivity<LinkTestResultActivity>()
            assertBrowserIsOpened(context.getString(R.string.link_test_result_report_link_url)) {
                linkTestResultRobot.clickReportLink()
            }
        }
    }

    private val contactCaseOnlyIsolation = IsolationState(
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(
            exposureDate = LocalDate.now(),
            notificationDate = LocalDate.now(),
            expiryDate = LocalDate.now().plusDays(1)
        )
    )
}
