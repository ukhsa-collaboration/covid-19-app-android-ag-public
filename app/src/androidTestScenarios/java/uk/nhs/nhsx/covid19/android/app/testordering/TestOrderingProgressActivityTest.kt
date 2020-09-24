package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingProgressRobot

class TestOrderingProgressActivityTest : EspressoTest() {

    private val testOrderingProgressRobot = TestOrderingProgressRobot()

    @Test
    fun opensBrowser() = notReported {
        testAppContext.virologyTestingApi.shouldPass = true

        assertBrowserIsOpened("about:blank") {
            startTestActivity<TestOrderingProgressActivity>()
        }
    }

    @Test
    fun clickTryAgainButtonOnResponseFailure() = notReported {
        testAppContext.virologyTestingApi.shouldPass = false

        startTestActivity<TestOrderingProgressActivity>()

        testOrderingProgressRobot.checkActivityIsDisplayed()

        testOrderingProgressRobot.checkErrorIsDisplayed()

        testAppContext.virologyTestingApi.shouldPass = true

        assertBrowserIsOpened("about:blank") {
            testOrderingProgressRobot.clickTryAgainButton()
        }
    }
}
