package uk.nhs.nhsx.covid19.android.app.localstats

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot

class FetchLocalDataProgressActivityTest : EspressoTest() {

    private val progressRobot = ProgressRobot()

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<FetchLocalDataProgressActivity>()
        progressRobot.checkActivityIsDisplayed()
    }
}
