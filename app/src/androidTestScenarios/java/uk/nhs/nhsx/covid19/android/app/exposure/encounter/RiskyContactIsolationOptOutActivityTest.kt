package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class RiskyContactIsolationOptOutActivityTest : EspressoTest() {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val riskyContactIsolationOptOutRobot = RiskyContactIsolationOptOutRobot()
    private val statusRobot = StatusRobot()

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
    fun clickPrimaryButtonShouldNavigateToExternalLink() {
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        assertBrowserIsOpened(context.getString(string.risky_contact_opt_out_primary_button_url)) {
            riskyContactIsolationOptOutRobot.clickPrimaryButton_opensInExternalBrowser()
        }
    }

    @Test
    fun clickGuidanceShouldNavigateToExternalLink() {
        startTestActivity<RiskyContactIsolationOptOutActivity>()

        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()

        assertBrowserIsOpened(context.getString(string.risky_contact_opt_out_further_advice_link_url)) {
            riskyContactIsolationOptOutRobot.clickGuidance_opensInExternalBrowser()
        }
    }
}
