package uk.nhs.nhsx.covid19.android.app.qrcode

import android.content.Context
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector.Builder

class AndroidBarcodeDetectorBuilder(
    private val context: Context
) : BarcodeDetectorBuilder {

    override fun build(): Detector<Barcode>? =
        Builder(context).setBarcodeFormats(Barcode.QR_CODE).build()
}
