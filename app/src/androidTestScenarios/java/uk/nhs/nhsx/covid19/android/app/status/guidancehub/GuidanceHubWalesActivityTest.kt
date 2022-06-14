package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.GuidanceHubWalesRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

@RunWith(Parameterized::class)
class GuidanceHubWalesActivityTest(override val configuration: TestConfiguration) : EspressoTest(), HasActivity {
    override val containerId = R.id.guidanceHubWalesContainer

    private val guidanceHubWalesRobot = GuidanceHubWalesRobot()
    private val browserRobot = BrowserRobot()

    @Test
    @Reported
    fun showGuidanceHubForWales() = reporter(
        scenario = "Covid Guidance hub",
        title = "Display guidance hub screen for Wales",
        description = "Guidance Hub – Wales",
        kind = SCREEN
    ) {
        startTestActivity<GuidanceHubWalesActivity>()

        guidanceHubWalesRobot.checkActivityIsDisplayed()

        step(
            stepName = "Show covid guidance hub screen",
            stepDescription = "User navigates to guidance hub page for Wales"
        )
    }

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
