package uk.nhs.nhsx.covid19.android.app.flow.analytics

import kotlin.test.assertNull
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Success
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResultActivity
import uk.nhs.nhsx.covid19.android.app.qrcode.QrScannerActivity
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrCodeScanResultRobot

class QrCodeAnalyticsTest : AnalyticsTest() {

    private val qrCodeScanResultRobot = QrCodeScanResultRobot()

    @Test
    fun countsNumberOfCheckIns() {
        checkInQrCode()

        assertOnFields {
            assertEquals(1, Metrics::checkedIn)
        }
    }

    @Test
    fun countsNumberOfCanceledCheckIns() {
        cancelQrCheckIn()

        assertOnFields {
            assertEquals(1, Metrics::canceledCheckIn)
        }
    }

    private fun checkInQrCode() {
        startTestActivity<QrScannerActivity>()

        testAppContext.barcodeDetectorProvider.qrCode = validQrCode

        waitFor { assertNull(testAppContext.barcodeDetectorProvider.qrCode) }
    }

    private fun cancelQrCheckIn() {
        startTestActivity<QrCodeScanResultActivity> {
            putExtra(QrCodeScanResultActivity.SCAN_RESULT, Success("venue"))
        }

        qrCodeScanResultRobot.cancelCheckIn()
    }

    companion object {
        private const val validQrCode =
            "UKC19TRACING:1:eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjMifQ.eyJpZCI6IjRXVDU5TTVZIiwib3BuIjoiR292ZXJubWVudCBPZmZpY2UgT2YgSHVtYW4gUmVzb3VyY2VzIn0.ZIvwm9rxiRTm4o-koafL6Bzre9pakcyae8m6_MSyvAl-CFkUgfm6gcXYn4gg5OScKZ1-XayHBGwEdps0RKXs4g"
    }
}
