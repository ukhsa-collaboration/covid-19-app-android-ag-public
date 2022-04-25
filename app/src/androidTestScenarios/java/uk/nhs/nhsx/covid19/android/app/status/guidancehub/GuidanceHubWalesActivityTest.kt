package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.GuidanceHubWalesRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

class GuidanceHubWalesActivityTest : EspressoTest(), HasActivity {
    override val containerId = R.id.guidanceHubWalesContainer

    private val guidanceHubWalesRobot = GuidanceHubWalesRobot()
    private val browserRobot = BrowserRobot()

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<GuidanceHubWalesActivity>()
        checkActivityIsDisplayed()
    }

    @Test
    fun clickItemOne_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubWalesActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubWalesRobot.clickItemOne()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemTwo_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubWalesActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubWalesRobot.clickItemTwo()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemThree_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubWalesActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubWalesRobot.clickItemThree()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemFour_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubWalesActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubWalesRobot.clickItemFour()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemFive_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubWalesActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubWalesRobot.clickItemFive()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemSix_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubWalesActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubWalesRobot.clickItemSix()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemSeven_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubWalesActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubWalesRobot.clickItemSeven()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }
}
