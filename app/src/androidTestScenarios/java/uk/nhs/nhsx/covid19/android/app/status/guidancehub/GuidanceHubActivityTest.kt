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
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.GuidanceHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

@RunWith(Parameterized::class)
class GuidanceHubActivityTest(override val configuration: TestConfiguration) : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.guidanceHubContainer

    private val guidanceHubRobot = GuidanceHubRobot()
    private val browserRobot = BrowserRobot()

    @Test
    @Reported
    fun showGuidanceHubForEngland() = reporter(
        scenario = "Covid Guidance hub",
        title = "Display guidance hub screen",
        description = "Guidance Hub – England",
        kind = SCREEN
    ) {
        startTestActivity<GuidanceHubActivity>()

        guidanceHubRobot.checkActivityIsDisplayed()

        step(
            stepName = "Show covid guidance hub screen",
            stepDescription = "User navigates to guidance hub page for England"
        )
    }

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<GuidanceHubActivity>()
        checkActivityIsDisplayed()
    }

    @Test
    fun clickItemOneForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemOne()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemTwoForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemTwo()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemThreeForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemThree()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemFourForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemFour()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemFiveForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemFive()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemSixForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemSix()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemSevenForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemSeven()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemEightForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemEight()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemWithNewLabelForEngland_shouldRemoveNewLabelOnNavigateBack() {
        startTestActivity<GuidanceHubActivity>()

        waitFor { guidanceHubRobot.checkNewLabelIsDisplayed(true) }

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemSeven()

            waitFor { browserRobot.checkActivityIsDisplayed() }
            testAppContext.device.pressBack()
        }

        waitFor { guidanceHubRobot.checkNewLabelIsDisplayed(false) }
    }
}
