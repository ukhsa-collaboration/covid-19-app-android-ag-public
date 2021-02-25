package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class OrderTest(
    private val espressoTest: EspressoTest
) {

    private val testOrderingRobot = TestOrderingRobot()
    private val browserRobot = BrowserRobot()
    private val statusRobot = StatusRobot()

    operator fun invoke(pollingToken: String? = null) = runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
        if (pollingToken != null) {
            espressoTest.testAppContext.virologyTestingApi.pollingToken = pollingToken
        }

        waitFor { testOrderingRobot.checkActivityIsDisplayed() }
        testOrderingRobot.clickOrderTestButton()
        waitFor { browserRobot.checkActivityIsDisplayed() }
        browserRobot.clickCloseButton()
        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
