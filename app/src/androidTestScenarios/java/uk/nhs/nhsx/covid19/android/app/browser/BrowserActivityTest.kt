package uk.nhs.nhsx.covid19.android.app.browser

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot

class BrowserActivityTest : EspressoTest() {

    private val browserRobot = BrowserRobot()

    @Test
    fun startActivityWithoutUrl_shouldDisplayActivity() {
        startTestActivity<BrowserActivity>()

        waitFor { browserRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startActivityWithUrl_shouldDisplayActivity() {
        startTestActivity<BrowserActivity> {
            putExtra("EXTRA_URL", "www.google.com")
        }

        waitFor { browserRobot.checkActivityIsDisplayed() }
    }
}
