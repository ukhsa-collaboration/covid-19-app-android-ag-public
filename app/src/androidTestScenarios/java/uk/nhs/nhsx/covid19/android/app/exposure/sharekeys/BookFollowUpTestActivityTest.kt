package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BookFollowUpTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import kotlin.test.assertTrue

class BookFollowUpTestActivityTest : EspressoTest() {

    private val bookFollowUpTestRobot = BookFollowUpTestRobot()
    private val testOrderingRobot = TestOrderingRobot()

    @Test
    fun testActivityNavigatesToTestOrderingActivity() = notReported {
        startTestActivity<BookFollowUpTestActivity>()

        waitFor { bookFollowUpTestRobot.checkActivityIsDisplayed() }

        bookFollowUpTestRobot.clickBookFollowUpTestButton()

        waitFor { testOrderingRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun testActivityCloseButtonFinishesActivity() = notReported {
        val activity = startTestActivity<BookFollowUpTestActivity>()

        waitFor { bookFollowUpTestRobot.checkActivityIsDisplayed() }

        bookFollowUpTestRobot.clickCloseButton()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
