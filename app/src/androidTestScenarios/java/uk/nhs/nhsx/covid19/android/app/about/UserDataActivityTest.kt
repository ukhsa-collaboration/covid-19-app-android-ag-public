package uk.nhs.nhsx.covid19.android.app.about

import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PostCodeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UserDataRobot
import java.util.concurrent.TimeUnit.SECONDS

class UserDataActivityTest : EspressoTest() {
    private val moreAboutAppRobot = MoreAboutAppRobot()
    private val userDataRobot = UserDataRobot()
    private val postCodeRobot = PostCodeRobot()

    @Test
    fun myDataScreenShows() = notReported {
        startTestActivity<MoreAboutAppActivity>()

        moreAboutAppRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickOnSetDataOpensMyDataScreen() = notReported {
        startTestActivity<MoreAboutAppActivity>()

        moreAboutAppRobot.checkActivityIsDisplayed()

        moreAboutAppRobot.clickSeeData()

        userDataRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickOnDeleteUserDataOpensPostCodeScreen() = notReported {
        testAppContext.setAuthenticated(true)

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        userDataRobot.userClicksOnDeleteAllDataButton()

        userDataRobot.userClicksDeleteDataOnDialog()

        await.atMost(10, SECONDS) until { postCodeRobot.isActivityDisplayed() }

        postCodeRobot.checkActivityIsDisplayed()
    }
}
