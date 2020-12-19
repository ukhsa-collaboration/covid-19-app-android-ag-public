package uk.nhs.nhsx.covid19.android.app.qrcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.CameraPermissionNotGranted
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.InvalidContent
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Scanning
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.ScanningNotSupported
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Success
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

typealias RemoveVisitResult = Unit

class VenueCheckInViewModel @Inject constructor(
    private val visitedVenuesStorage: VisitedVenuesStorage,
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val clock: Clock
) : ViewModel() {

    private val viewStateLiveData = MutableLiveData<ViewState>()
    fun viewState(): LiveData<ViewState> = viewStateLiveData

    private val isViewStateCameraPermissionNotGrantedLiveData = SingleLiveEvent<Unit>()
    fun isViewStateCameraPermissionNotGranted(): LiveData<Unit> = isViewStateCameraPermissionNotGrantedLiveData

    private val visitRemovedResult = SingleLiveEvent<RemoveVisitResult>()
    fun getVisitRemovedResult(): LiveData<RemoveVisitResult> = visitRemovedResult

    fun onCreate(scanResult: QrCodeScanResult) {
        viewStateLiveData.postValue(
            when (scanResult) {
                is Success -> ViewState.Success(scanResult.venueName, LocalDateTime.now(clock))
                CameraPermissionNotGranted -> ViewState.CameraPermissionNotGranted
                Scanning, InvalidContent -> ViewState.InvalidContent
                ScanningNotSupported -> ViewState.ScanningNotSupported
            }
        )
    }

    fun removeLastVisit() {
        viewModelScope.launch {
            visitedVenuesStorage.removeLastVisit()
            visitRemovedResult.postValue(RemoveVisitResult)
            analyticsEventProcessor.track(CanceledCheckIn)
        }
    }

    fun onResume() {
        if (viewStateLiveData.value == ViewState.CameraPermissionNotGranted) {
            isViewStateCameraPermissionNotGrantedLiveData.postCall()
        }
    }

    sealed class ViewState {
        data class Success(val venueName: String, val currentDateTime: LocalDateTime) : ViewState()
        object CameraPermissionNotGranted : ViewState()
        object InvalidContent : ViewState()
        object ScanningNotSupported : ViewState()
    }
}
