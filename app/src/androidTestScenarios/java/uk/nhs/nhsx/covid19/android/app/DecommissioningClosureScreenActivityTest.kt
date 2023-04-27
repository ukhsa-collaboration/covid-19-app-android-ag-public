package uk.nhs.nhsx.covid19.android.app

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DecommissioningClosureScreenRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.context

class DecommissioningClosureScreenActivityTest : EspressoTest() {

    private val decommissioningClosureScreenRobot = DecommissioningClosureScreenRobot()

    @Test
    fun isActivityDisplayed() {
        startTestActivity<DecommissioningClosureScreenActivity>()
        decommissioningClosureScreenRobot.checkActivityIsDisplayed()
    }

    @Test
    fun linkOneOpensInBrowser() {
        assertLinkViewOpensInBrowserWhenClicked(
            context.getString(string.decommissioning_closure_screen_url_1_link),
            context.getString(string.decommissioning_closure_screen_url_1_label)
        )
    }

    @Test
    fun linkTwoOpensInBrowser() {
        assertLinkViewOpensInBrowserWhenClicked(
            context.getString(string.decommissioning_closure_screen_url_2_link),
            context.getString(string.decommissioning_closure_screen_url_2_label)
        )
    }

    @Test
    fun linkThreeOpensInBrowser() {
        assertLinkViewOpensInBrowserWhenClicked(
            context.getString(string.decommissioning_closure_screen_url_3_link),
            context.getString(string.decommissioning_closure_screen_url_3_label)
        )
    }

    @Test
    fun linkFourOpensInBrowser() {
        assertLinkViewOpensInBrowserWhenClicked(
            context.getString(string.decommissioning_closure_screen_url_4_link),
            context.getString(string.decommissioning_closure_screen_url_4_label)
        )
    }

    @Test
    fun linkFiveOpensInBrowser() {
        assertLinkViewOpensInBrowserWhenClicked(
            context.getString(string.decommissioning_closure_screen_url_5_link),
            context.getString(string.decommissioning_closure_screen_url_5_label)
        )
    }

    private fun assertLinkViewOpensInBrowserWhenClicked(link: String, label: String) {
        startTestActivity<DecommissioningClosureScreenActivity>()

        waitFor { decommissioningClosureScreenRobot.checkActivityIsDisplayed() }

        assertBrowserIsOpened(link) {
            decommissioningClosureScreenRobot.clickExternalUrlSectionLink_opensInExternalBrowser(label)
        }
    }
}
