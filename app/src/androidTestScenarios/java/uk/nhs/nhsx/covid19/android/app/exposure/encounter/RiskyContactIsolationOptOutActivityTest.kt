package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

class RiskyContactIsolationOptOutActivityTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val riskyContactIsolationOptOutRobot = RiskyContactIsolationOptOutRobot()
    private val statusRobot = StatusRobot()

    @Before
    fun setUp() {
        givenLocalAuthorityIsInEngland()
    }

    @Test
    fun activityIsDisplayed() {
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickSecondaryButtonShouldNavigateToHomeScreen() {
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        riskyContactIsolationOptOutRobot.clickSecondaryButton()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickPrimaryButtonShouldNavigateToExternalLinkForEngland() {
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        riskyContactIsolationOptOutRobot.checkPrimaryButtonUrl(ENGLAND)
    }

    @Test
    fun clickPrimaryButtonShouldNavigateToExternalLinkForWales() {
        givenLocalAuthorityIsInWales()
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        riskyContactIsolationOptOutRobot.checkPrimaryButtonUrl(WALES)
    }

    @Test
    fun clickGuidanceShouldNavigateToExternalLink() {
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        riskyContactIsolationOptOutRobot.checkGuidanceUrl()
    }

    @Test
    fun verifyEnglandStringAreDisplayed() {
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        riskyContactIsolationOptOutRobot.checkEnglandAdviceCopiesAreDisplayed()
    }

    @Test
    fun verifyWalesStringAreDisplayed() {
        givenLocalAuthorityIsInWales()
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        waitFor { riskyContactIsolationOptOutRobot.checkWalesAdviceCopiesAreDisplayed() }
    }
}
