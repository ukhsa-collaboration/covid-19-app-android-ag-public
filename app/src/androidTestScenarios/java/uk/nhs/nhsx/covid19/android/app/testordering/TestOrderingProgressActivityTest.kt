package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot

class TestOrderingProgressActivityTest : EspressoTest() {

    private val testOrderingProgressRobot = ProgressRobot()

    @Test
    fun opensBrowser() {
        assertBrowserIsOpened("about:blank") {
            startTestActivity<TestOrderingProgressActivity>()
        }
    }

    @Test
    fun clickTryAgainButtonOnResponseFailure() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL

        startTestActivity<TestOrderingProgressActivity>()

        testOrderingProgressRobot.checkActivityIsDisplayed()

        testOrderingProgressRobot.checkErrorIsDisplayed()

        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED

        assertBrowserIsOpened("about:blank") {
            testOrderingProgressRobot.clickTryAgainButton()
        }
    }
}
