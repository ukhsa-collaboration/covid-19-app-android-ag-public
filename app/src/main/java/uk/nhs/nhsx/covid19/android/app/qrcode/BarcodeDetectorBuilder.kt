package uk.nhs.nhsx.covid19.android.app.qrcode

import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode

interface BarcodeDetectorBuilder {

    fun build(): Detector<Barcode>?
}
