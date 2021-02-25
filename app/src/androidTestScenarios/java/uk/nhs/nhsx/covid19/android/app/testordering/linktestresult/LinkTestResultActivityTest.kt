package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import java.time.Instant
import java.time.LocalDate

class LinkTestResultActivityTest : EspressoTest() {

    private val linkTestResultRobot = LinkTestResultRobot()

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
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

    private val contactCaseOnlyIsolation = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(Instant.now(), null, LocalDate.now().plusDays(1))
    )
}
