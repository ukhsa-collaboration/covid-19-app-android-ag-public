package uk.nhs.nhsx.covid19.android.app.qrcode

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class QrCodeScanResult : Parcelable {

    @Parcelize
    object Scanning : QrCodeScanResult()

    @Parcelize
    data class Success(val venueName: String) : QrCodeScanResult()

    @Parcelize
    object CameraPermissionNotGranted : QrCodeScanResult()

    @Parcelize
    object InvalidContent : QrCodeScanResult()

    @Parcelize
    object ScanningNotSupported : QrCodeScanResult()
}
