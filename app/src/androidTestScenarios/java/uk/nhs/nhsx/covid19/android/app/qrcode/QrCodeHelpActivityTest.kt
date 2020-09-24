package uk.nhs.nhsx.covid19.android.app.qrcode

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrCodeHelpRobot

class QrCodeHelpActivityTest : EspressoTest() {

    private val qrCodeHelpRobot = QrCodeHelpRobot()

    @Test
    fun checkActivityIsDisplayed() = reporter(
        scenario = "Venue check-in",
        title = "Help",
        description = "During check-in or after a failed attempt to check-in the user can visit this screen to find help",
        kind = SCREEN
    ) {
        startTestActivity<QrCodeHelpActivity>()

        qrCodeHelpRobot.checkActivityIsDisplayed()

        step(
            stepName = "Start",
            stepDescription = "User is presented a screen that supports them using the venue check-in feature correctly"
        )
    }
}
