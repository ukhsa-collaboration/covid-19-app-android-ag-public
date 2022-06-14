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
    fun clickItemGuidanceForEngland_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemGuidanceForEngland()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemCheckSymptoms_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemCheckSymptoms()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemLatest_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemLatest()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemPositiveTestResult_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemPositiveTestResult()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemTravellingAbroad_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemTravellingAbroad()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemCheckSSP_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemCheckSSP()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun clickItemEnquiries_shouldOpenInExternalBrowser() {
        startTestActivity<GuidanceHubActivity>()

        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            guidanceHubRobot.clickItemEnquiries()

            waitFor { browserRobot.checkActivityIsDisplayed() }
        }
    }
}
