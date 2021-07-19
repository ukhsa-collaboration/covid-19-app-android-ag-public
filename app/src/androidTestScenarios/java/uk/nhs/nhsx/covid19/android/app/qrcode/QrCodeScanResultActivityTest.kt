package uk.nhs.nhsx.covid19.android.app.qrcode

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.InvalidContent
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Scanning
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrCodeScanResultRobot

@RunWith(Parameterized::class)
class QrCodeScanResultActivityTest(override val configuration: TestConfiguration) : EspressoTest() {

    private val robot = QrCodeScanResultRobot()

    @Test
    fun showSuccess() {
        val venueName = "Sample Venue"

        startTestActivity<QrCodeScanResultActivity> {
            putExtra(
                QrCodeScanResultActivity.SCAN_RESULT,
                QrCodeScanResult.Success(venueName)
            )
        }

        waitFor { robot.checkAnimationIconIsDisplayed() }

        robot.checkSuccessTitleAndVenueIsDisplayed(venueName)
    }

    @Test
    fun showInvalidContent() {
        startTestActivity<QrCodeScanResultActivity> {
            putExtra(
                QrCodeScanResultActivity.SCAN_RESULT,
                Scanning
            )
        }

        robot.checkFailureIconIsDisplayed()

        robot.checkQrCodeNotRecognizedTitleIsDisplayed()
    }

    @Test
    @Reported
    fun showInvalidContentScanningResult() = reporter(
        scenario = "Venue check-in",
        title = "Invalid QRCode",
        description = "The QR code scanned by the user is not an official NHS QR code or is defect",
        kind = SCREEN
    ) {
        startTestActivity<QrCodeScanResultActivity> {
            putExtra(
                QrCodeScanResultActivity.SCAN_RESULT,
                InvalidContent
            )
        }

        robot.checkFailureIconIsDisplayed()

        robot.checkQrCodeNotRecognizedTitleIsDisplayed()

        step(
            stepName = "After scan attempt",
            stepDescription = "User is presented a screen that shows the scanned QR code was not recognized"
        )
    }

    @Test
    @Reported
    fun showScanningNotSupported() = reporter(
        scenario = "Venue check-in",
        title = "Unsupported Phone",
        description = "The user's phone does not support venue check-in",
        kind = SCREEN
    ) {
        startTestActivity<QrCodeScanResultActivity> {
            putExtra(
                QrCodeScanResultActivity.SCAN_RESULT,
                QrCodeScanResult.ScanningNotSupported
            )
        }

        robot.checkFailureIconIsDisplayed()

        robot.checkNotSupportedTitleIsDisplayed()

        step(
            stepName = "Start",
            stepDescription = "User is presented a screen that informs them their phone does not support venue check-in"
        )
    }
}
