package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.FAIL_SUCCEED_LOOP
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot

class TestOrderingProgressActivityTest : EspressoTest() {

    private val testOrderingProgressRobot = ProgressRobot()

    @Test
    fun opensBrowser() = notReported {
        assertBrowserIsOpened("about:blank") {
            startTestActivity<TestOrderingProgressActivity>()
        }
    }

    @Test
    fun clickTryAgainButtonOnResponseFailure() = notReported {
        MockApiModule.behaviour.responseType = FAIL_SUCCEED_LOOP

        startTestActivity<TestOrderingProgressActivity>()

        testOrderingProgressRobot.checkActivityIsDisplayed()

        testOrderingProgressRobot.checkErrorIsDisplayed()

        assertBrowserIsOpened("about:blank") {
            testOrderingProgressRobot.clickTryAgainButton()
        }
    }
}
