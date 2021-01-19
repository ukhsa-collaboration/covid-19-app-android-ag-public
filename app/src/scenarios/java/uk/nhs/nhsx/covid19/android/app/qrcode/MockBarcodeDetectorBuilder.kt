package uk.nhs.nhsx.covid19.android.app.qrcode

import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode

class MockBarcodeDetectorBuilder : BarcodeDetectorBuilder {

    var qrCode: String? = null

    override fun build(): Detector<Barcode> =
        object : Detector<Barcode>() {
            override fun detect(frame: Frame): SparseArray<Barcode> {
                val result = qrCode?.let {
                    SparseArray<Barcode>().apply {
                        append(
                            0,
                            Barcode().apply {
                                format = Barcode.QR_CODE
                                rawValue = qrCode
                            }
                        )
                    }
                } ?: SparseArray()
                qrCode = null
                return result
            }
        }
}
