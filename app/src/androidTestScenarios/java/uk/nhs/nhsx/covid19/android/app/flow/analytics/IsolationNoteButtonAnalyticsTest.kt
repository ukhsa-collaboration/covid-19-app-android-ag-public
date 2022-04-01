package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper

class IsolationNoteButtonAnalyticsTest : AnalyticsTest(), IsolationSetupHelper {

    private val statusRobot = StatusRobot()
    private val isolationHubRobot = IsolationHubRobot()
    private val browserRobot = BrowserRobot()

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Test
    fun whenClickingIsolationNoteItemMultipleTimes_forEachClick_increaseDidAccessSelfIsolation() =
        runWithFeatureEnabled(SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES) {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            givenContactIsolation()

            startTestActivity<StatusActivity>()

            statusRobot.clickIsolationHub()

            isolationHubRobot.checkActivityIsDisplayed()
            isolationHubRobot.clickItemIsolationNote()

            waitFor { browserRobot.checkActivityIsDisplayed() }
            browserRobot.clickCloseButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
            statusRobot.clickIsolationHub()

            isolationHubRobot.checkActivityIsDisplayed()
            isolationHubRobot.clickItemIsolationNote()

            assertOnFields(implicitlyAssertNotPresent = false) {
                assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
                assertPresent(Metrics::receivedActiveIpcToken)
                assertEquals(2, Metrics::didAccessSelfIsolationNoteLink)
            }
        }
    }
}
