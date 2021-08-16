package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

class IsolationHubScenarioTest : EspressoTest() {

    private val statusRobot = StatusRobot()
    private val isolationHubRobot = IsolationHubRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val browserRobot = BrowserRobot()

    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenInActiveIsolationAndNoBookTestTypeVenueVisitStored_bookATest_navigateToStatusActivity() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
            testAppContext.setState(isolationHelper.contactCase().asIsolation())

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()
            statusRobot.clickIsolationHub()

            isolationHubRobot.checkActivityIsDisplayed()
            isolationHubRobot.checkItemBookTestIsDisplayed()
            isolationHubRobot.clickItemBookATest()

            testOrderingRobot.checkActivityIsDisplayed()
            testOrderingRobot.clickOrderTestButton()

            waitFor { browserRobot.checkActivityIsDisplayed() }
            browserRobot.clickCloseButton()

            statusRobot.checkActivityIsDisplayed()
        }
    }
}
