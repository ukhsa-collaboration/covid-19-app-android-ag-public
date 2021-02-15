package uk.nhs.nhsx.covid19.android.app.availability

import kotlin.test.assertTrue
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

    @Test
    fun clickAskMeLaterWhenNotStartedFromNotification_finishes() = notReported {
        val activity = startTestActivity<UpdateRecommendedActivity> {
            putExtra(UpdateRecommendedActivity.STARTED_FROM_NOTIFICATION, false)
        }

        updateRecommendedRobot.checkActivityIsDisplayed()

        updateRecommendedRobot.clickAskMeLater()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun clickAskMeLaterWhenStartedFromNotification_finishes() = notReported {
        val activity = startTestActivity<UpdateRecommendedActivity> {
            putExtra(UpdateRecommendedActivity.STARTED_FROM_NOTIFICATION, true)
        }

        updateRecommendedRobot.checkActivityIsDisplayed()

        updateRecommendedRobot.clickAskMeLater()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun backPressed_finished() = notReported {
        val activity = startTestActivity<UpdateRecommendedActivity> {
            putExtra(UpdateRecommendedActivity.STARTED_FROM_NOTIFICATION, false)
        }

        updateRecommendedRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
