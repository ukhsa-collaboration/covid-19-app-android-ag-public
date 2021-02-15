package uk.nhs.nhsx.covid19.android.app.qrcode

import android.Manifest.permission.CAMERA
import android.content.pm.PackageManager
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Success
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrCodeScanResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrScannerRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertRobot

class QrCodeScanScenarioTest : EspressoTest() {

    @get:Rule
    var cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(CAMERA)

    private val qrScannerRobot = QrScannerRobot()
    private val qrCodeScanResultRobot = QrCodeScanResultRobot()
    private val venueAlertRobot = VenueAlertRobot()

    @After
    fun tearDown() {
        testAppContext.permissionsManager.clear()
    }

    @Test
    fun checkInWithValidQrCodeAndCameraPermissionEnabled_shouldReceiveRiskyVenueWarning() =
        reporter(
            scenario = "Venue check-in",
            title = "Happy path",
            description = "The user successfully scans an official NHS QR code and later receives warning about visited venue being risky",
            kind = FLOW
        ) {
            testAppContext.permissionsManager.addGrantedPermission(CAMERA)

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

    @Test
    fun checkInWithValidQrCodeAndCameraPermissionDisabled_shouldRequestPermission_onPermissionGranted_shouldReceiveRiskyVenueWarning() =
        notReported {
            startTestActivity<QrScannerActivity>()

            qrScannerRobot.checkActivityIsDisplayed()

            runBlocking {
                testAppContext.getVisitedVenuesStorage().finishLastVisitAndAddNewVenue(
                    Venue("ABCD1234", "ABCD1234")
                )
            }

            startTestActivity<QrCodeScanResultActivity> {
                putExtra(QrCodeScanResultActivity.SCAN_RESULT, Success("ABCD1234"))
            }

            qrCodeScanResultRobot.checkSuccessTitleIsDisplayed()

            qrCodeScanResultRobot.clickBackToHomeButton()

            runBlocking {
                testAppContext.getDownloadAndProcessRiskyVenues().invoke()
            }

            waitFor { venueAlertRobot.checkVenueTitleIsDisplayed() }
        }

    @Test
    fun checkInWithValidQrCodeAndCameraPermissionDisabled_shouldRequestPermission_onPermissionDenied_shouldShowPermissionDenied() =
        notReported {
            testAppContext.permissionsManager.setResponseForPermissionRequest(
                CAMERA,
                PackageManager.PERMISSION_DENIED
            )

            startTestActivity<QrScannerActivity>()

            waitFor { qrCodeScanResultRobot.checkPermissionDeniedTitleIsDisplayed() }
        }
}
