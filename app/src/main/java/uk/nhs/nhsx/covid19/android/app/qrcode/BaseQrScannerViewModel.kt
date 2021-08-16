package uk.nhs.nhsx.covid19.android.app.qrcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Scanning
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent

abstract class BaseQrScannerViewModel(
    private val qrCodeParser: QrCodeParser,
    private val visitedVenuesStorage: VisitedVenuesStorage,
    private val delay: Long,
    private val analyticsEventProcessor: AnalyticsEventProcessor
) : ViewModel() {
    var hasRequestedCameraPermission = false

    protected val qrCodeScanResult = SingleLiveEvent<QrCodeScanResult>()
    open fun getQrCodeScanResult(): LiveData<QrCodeScanResult> = qrCodeScanResult

    fun parseQrCode(rawValue: String) {
        viewModelScope.launch {
            val venue = try {
                qrCodeParser.parse(rawValue)
            } catch (exception: IllegalArgumentException) {
                qrCodeScanResult.postValue(Scanning)
                delay(delay)
                qrCodeScanResult.postValue(QrCodeScanResult.InvalidContent)
                return@launch
            }

            venueScanned(venue)
        }
    }

    private fun trackAnalytics() {
        analyticsEventProcessor.track(QrCodeCheckIn)
    }

    protected suspend fun venueScanned(venue: Venue) {
        trackAnalytics()
        visitedVenuesStorage.finishLastVisitAndAddNewVenue(venue)
        qrCodeScanResult.postValue(Scanning)
        delay(delay)
        qrCodeScanResult.postValue(QrCodeScanResult.Success(venueName = venue.organizationPartName))
    }

    companion object {
        const val DELAY_TO_SHOW_SCANNING_TEXT_IN_MS = 500L
    }
}
