package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DailyContactTestingConfirmationRobot

class DailyContactTestingConfirmationActivityTest : EspressoTest() {

    private val dailyContactTestingConfirmationRobot = DailyContactTestingConfirmationRobot()

    @Test
    fun clickContinue_pressDismiss_staysInActivity() {
        startTestActivity<DailyContactTestingConfirmationActivity>()

        dailyContactTestingConfirmationRobot.checkActivityIsDisplayed()

        dailyContactTestingConfirmationRobot.clickConfirmOptInToOpenDialog()

        waitFor { dailyContactTestingConfirmationRobot.checkDailyContactTestingOptInConfirmationDialogIsDisplayed() }

        dailyContactTestingConfirmationRobot.clickDialogCancel()

        waitFor { dailyContactTestingConfirmationRobot.checkActivityIsDisplayed() }
    }
}
