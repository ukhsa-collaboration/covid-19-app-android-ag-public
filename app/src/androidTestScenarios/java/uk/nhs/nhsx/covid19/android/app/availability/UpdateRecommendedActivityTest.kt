package uk.nhs.nhsx.covid19.android.app.availability

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UpdateRecommendedRobot

class UpdateRecommendedActivityTest : EspressoTest() {

    private val updateRecommendedRobot = UpdateRecommendedRobot()

    @Test
    fun showScreen() = notReported {
        startTestActivity<UpdateRecommendedActivity>()

        updateRecommendedRobot.checkActivityIsDisplayed()
    }
}
