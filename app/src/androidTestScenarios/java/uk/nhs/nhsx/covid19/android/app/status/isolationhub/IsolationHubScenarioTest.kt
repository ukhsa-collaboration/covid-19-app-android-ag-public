package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

class IsolationHubScenarioTest : EspressoTest(), IsolationSetupHelper, LocalAuthoritySetupHelper {

    private val statusRobot = StatusRobot()
    private val isolationHubRobot = IsolationHubRobot()
    private val testOrderingRobot = TestOrderingRobot()
    private val browserRobot = BrowserRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenInActiveIsolationAndNoBookTestTypeVenueVisitStored_bookATest_navigateToStatusActivity() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
            givenContactIsolation()
            navigateToIsolationHub()

            with(isolationHubRobot) {
                checkActivityIsDisplayed()
                checkItemBookTestIsDisplayed()
                clickItemBookATest()
            }

            testOrderingRobot.checkActivityIsDisplayed()
            testOrderingRobot.clickOrderTestButton()

            waitFor { browserRobot.checkActivityIsDisplayed() }
            browserRobot.clickCloseButton()

            statusRobot.checkActivityIsDisplayed()
        }
    }

    @Test
    fun whenInIsolation_clickIsolationNoteItem_openBrowserWithCorrectUrl() {
        givenContactIsolation()
        navigateToIsolationHub()

        isolationHubRobot.checkActivityIsDisplayed()
        isolationHubRobot.checkItemIsolationNoteIsDisplayed()

        assertBrowserIsOpened(R.string.link_isolation_note) {
            isolationHubRobot.clickItemIsolationNote()
        }
    }

    @Test
    fun whenInIsolation_clickIsolationNoteItem_closeBrowser_shouldShowStatusActivity() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            givenContactIsolation()
            navigateToIsolationHub()

            with(isolationHubRobot) {
                checkActivityIsDisplayed()
                checkItemIsolationNoteIsDisplayed()
                clickItemIsolationNote()
            }

            browserRobot.clickCloseButton()

            statusRobot.checkActivityIsDisplayed()
        }
    }

    private fun navigateToIsolationHub() {
        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()
        statusRobot.clickIsolationHub()
    }
}
