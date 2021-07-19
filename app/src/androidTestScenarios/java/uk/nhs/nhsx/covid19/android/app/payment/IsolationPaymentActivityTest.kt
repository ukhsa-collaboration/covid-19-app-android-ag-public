package uk.nhs.nhsx.covid19.android.app.payment

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationPaymentRobot

class IsolationPaymentActivityTest : EspressoTest() {

    private val isolationPaymentRobot = IsolationPaymentRobot()

    @Test
    fun showScreen() {
        startTestActivity<IsolationPaymentActivity>()

        isolationPaymentRobot.checkActivityIsDisplayed()
    }
}
