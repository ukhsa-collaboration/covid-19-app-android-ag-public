package uk.nhs.nhsx.covid19.android.app.edgecases

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.edgecases.TabletNotSupportedRobot

class TabletNotSupportedActivityTest : EspressoTest() {

    private val tabletNotSupportedRobot = TabletNotSupportedRobot()

    @Test
    fun showScreen() = notReported {
        startTestActivity<TabletNotSupportedActivity>()

        tabletNotSupportedRobot.checkActivityIsDisplayed()
    }
}
