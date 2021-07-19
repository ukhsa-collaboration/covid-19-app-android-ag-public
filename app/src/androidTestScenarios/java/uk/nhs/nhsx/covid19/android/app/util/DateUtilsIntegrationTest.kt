package uk.nhs.nhsx.covid19.android.app.util

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResultActivity
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationExpirationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QrCodeScanResultRobot
import java.time.Instant
import java.time.LocalDate

class DateUtilsIntegrationTest : EspressoTest() {

    private val qrCodeScanRobot = QrCodeScanResultRobot()
    private val isolationExpirationRobot = IsolationExpirationRobot()
    private val currentDateTime = Instant.parse("2020-12-11T13:00:00Z")

    @Test
    fun qrCodeSuccessDateTime_inChineseLocale_returnsCorrectFormat() {
        testAppContext.setLocale("zh")
        testAppContext.clock.currentInstant = currentDateTime

        startTestActivity<QrCodeScanResultActivity> {
            putExtra(
                QrCodeScanResultActivity.SCAN_RESULT,
                QrCodeScanResult.Success("Sample Venue")
            )
        }

        qrCodeScanRobot.checkDateTimeFormat("2020年12月11日 13:00")
    }

    @Test
    fun isolationExpirationDate_inChineseLocale_returnsCorrectFormat() {
        testAppContext.setLocale("zh")
        testAppContext.clock.currentInstant = currentDateTime

        val expiryDate = LocalDate.now(testAppContext.clock)

        startTestActivity<IsolationExpirationActivity> {
            putExtra(IsolationExpirationActivity.EXTRA_EXPIRY_DATE, expiryDate.toString())
        }

        waitFor { isolationExpirationRobot.checkDateFormat("2020年12月10日") }
    }
}
