package uk.nhs.nhsx.covid19.android.app.qrcode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage

class QrScannerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val qrCodeParser = mockk<QrCodeParser>(relaxed = true)
    private val qrCodeScanResult = mockk<Observer<QrCodeScanResult>>(relaxed = true)
    private val visitedVenuesStorage = mockk<VisitedVenuesStorage>(relaxUnitFun = true)
    private val analyticsManager = mockk<AnalyticsEventProcessor>(relaxed = true)
    private val sut = QrScannerViewModel(qrCodeParser, visitedVenuesStorage, 0, analyticsManager)

    @Test
    fun `calls QrCodeParser parse`() {
        sut.parseQrCode(QR_CODE_PAYLOAD_INVALID)

        verify { qrCodeParser.parse(QR_CODE_PAYLOAD_INVALID) }
    }

    @Test
    fun `on invalid code returns InvalidContent`() {
        sut.getQrCodeScanResult().observeForever(qrCodeScanResult)
        every { qrCodeParser.parse(QR_CODE_PAYLOAD_INVALID) } throws IllegalArgumentException()

        sut.parseQrCode(QR_CODE_PAYLOAD_INVALID)

        verify { qrCodeScanResult.onChanged(QrCodeScanResult.InvalidContent) }
    }

    @Test
    fun `on valid code returns Success`() = runBlocking {
        sut.getQrCodeScanResult().observeForever(qrCodeScanResult)
        mockValidVenue()

        sut.parseQrCode(QR_CODE_PAYLOAD_VALID)

        verifyOrder {
            qrCodeScanResult.onChanged(QrCodeScanResult.Scanning)
            qrCodeScanResult.onChanged(QrCodeScanResult.Success(ORGANIZATION_NAME))
        }
    }

    @Test
    fun `on valid code saves venue to venues storage`() = runBlocking {
        val venue = mockValidVenue()

        sut.parseQrCode(QR_CODE_PAYLOAD_VALID)

        coVerify { visitedVenuesStorage.finishLastVisitAndAddNewVenue(venue) }
    }

    @Test
    fun `on valid code triggers checkedIn analytics event`() = runBlocking {
        mockValidVenue()

        sut.parseQrCode(QR_CODE_PAYLOAD_VALID)

        verify { analyticsManager.track(QrCodeCheckIn) }
    }

    private fun mockValidVenue(): Venue {
        val venue = mockk<Venue>()
        every { venue.organizationPartName } returns ORGANIZATION_NAME
        every { qrCodeParser.parse(QR_CODE_PAYLOAD_VALID) } returns venue
        return venue
    }

    companion object {
        const val QR_CODE_PAYLOAD_INVALID = "invalid"
        const val QR_CODE_PAYLOAD_VALID = "valid"
        const val ORGANIZATION_NAME = "sample name"
    }
}
