package uk.nhs.nhsx.covid19.android.app.qrcode

import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrCodeScanResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrScannerRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertRobot

class QrCodeScanScenarioTest : EspressoTest() {

    @get:Rule
    var cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val statusRobot = StatusRobot()
    private val qrScannerRobot = QrScannerRobot()
    private val qrCodeScanResultRobot = QrCodeScanResultRobot()
    private val venueAlertRobot = VenueAlertRobot()

    @Test
    fun checkInWithValidQrCodeAndReceiveRiskyVenueWarning() = reporter(
        scenario = "Venue check-in",
        title = "Happy path",
        description = "The user successfully scans an official NHS QR code and later receives warning about visited venue being risky",
        kind = FLOW
    ) {
        startTestActivity<QrScannerActivity>()

        qrScannerRobot.checkActivityIsDisplayed()

        step(
            stepName = "Scanning QR code",
            stepDescription = "User is shown a screen to scan a QR code"
        )

        runBlocking {
            testAppContext.getVisitedVenuesStorage().finishLastVisitAndAddNewVenue(
                Venue("ABCD1234", "ABCD1234")
            )
        }

        startTestActivity<QrCodeScanResultActivity> {
            putExtra(QrCodeScanResultActivity.SCAN_RESULT, QrCodeScanResult.Success("ABCD1234"))
        }

        qrCodeScanResultRobot.checkSuccessTitleIsDisplayed()

        step(
            stepName = "Successful scan",
            stepDescription = "User successfully checks in using an official NHS QR code. They tap 'Back to home'."
        )

        qrCodeScanResultRobot.clickBackToHomeButton()

        runBlocking {
            testAppContext.getDownloadAndProcessRiskyVenues().invoke()
        }

        waitFor { venueAlertRobot.checkVenueTitleIsDisplayed() }

        step(
            stepName = "Risky venue alert",
            stepDescription = "User is presented a screen that informs them they have recently visited a risky venue"
        )
    }
}
