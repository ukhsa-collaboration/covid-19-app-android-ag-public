package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot

class TestOrderingActivityTest : EspressoTest() {

    private val testOrderingRobot = TestOrderingRobot()
    private val testOrderingProgressRobot = ProgressRobot()

    @Test
    fun clickTryAgainButtonOnResponseFailure() = notReported {
        testAppContext.virologyTestingApi.shouldPass = false

        startTestActivity<TestOrderingActivity>()

        testOrderingRobot.checkActivityIsDisplayed()

        testOrderingRobot.clickOrderTestButton()

        testOrderingProgressRobot.checkActivityIsDisplayed()
    }
}
