package uk.nhs.nhsx.covid19.android.app.exposure

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot

class ShareKeysInformationActivityTest : EspressoTest() {

    private val shareKeysInformationRobot = ShareKeysInformationRobot()

    @Test
    fun showScreen() = notReported {
        startTestActivity<ShareKeysInformationActivity>()

        shareKeysInformationRobot.checkActivityIsDisplayed()
    }
}
