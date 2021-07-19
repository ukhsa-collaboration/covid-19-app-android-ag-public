package uk.nhs.nhsx.covid19.android.app.status.localmessage

import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.MockLocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalMessageRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

class LocalMessageFlowTest : EspressoTest() {
    private val localMessageRobot = LocalMessageRobot()
    private val statusRobot = StatusRobot()
    private val browserRobot = BrowserRobot()

    @Test
    fun localMessageInStatusScreen_navigatesToInfoScreen_clickLink_opensBrowserAndClose_clickBackToHome_navigatesToStatusScreen() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
            testAppContext.setLocalAuthority("E07000240")
            testAppContext.setPostCode("AL1")
            testAppContext.getLocalMessagesProvider().localMessages = MockLocalMessagesApi.successResponse

            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            statusRobot.clickLocalMessageBanner()

            waitFor { localMessageRobot.checkActivityIsDisplayed() }

            localMessageRobot.checkListSize(2)

            localMessageRobot.clickLink()

            waitFor { browserRobot.checkActivityIsDisplayed() }

            browserRobot.clickCloseButton()

            localMessageRobot.clickBackToHome()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun localMessageInStatusScreen_navigatesToInfoScreen_clickClose_navigatesToStatusScreen() {
        testAppContext.setLocalAuthority("E07000240")
        testAppContext.setPostCode("AL1")
        testAppContext.getLocalMessagesProvider().localMessages = MockLocalMessagesApi.successResponse

        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.clickLocalMessageBanner()

        waitFor { localMessageRobot.checkActivityIsDisplayed() }

        localMessageRobot.checkListSize(2)

        localMessageRobot.clickCloseButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
