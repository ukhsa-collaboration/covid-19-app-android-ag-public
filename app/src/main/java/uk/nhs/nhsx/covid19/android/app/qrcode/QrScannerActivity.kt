/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.nhs.nhsx.covid19.android.app.qrcode

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.lifecycle.Observer
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.Detector.Processor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.barcode.BarcodeDetector.Builder
import kotlinx.android.synthetic.main.activity_qr_code_scanner.closeButton
import kotlinx.android.synthetic.main.activity_qr_code_scanner.howToUseScannerHint
import kotlinx.android.synthetic.main.activity_qr_code_scanner.scannerSurfaceView
import kotlinx.android.synthetic.main.activity_qr_code_scanner.textHold
import kotlinx.android.synthetic.main.activity_qr_code_scanner.textMoreInfo
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Scanning
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.util.gone
import uk.nhs.nhsx.covid19.android.app.util.visible
import javax.inject.Inject

class QrScannerActivity : BaseActivity(R.layout.activity_qr_code_scanner) {

    @Inject
    lateinit var factory: ViewModelFactory<QrScannerViewModel>

    private val viewModel: QrScannerViewModel by viewModels { factory }

    private var barcodeDetector: BarcodeDetector? = null
    private var cameraSource: CameraSource? = null

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        appComponent.inject(this)

        viewModel.getQrCodeScanResult().observe(
            this,
            Observer { scanResult ->
                Timber.d("getQrCodeScanResult: $scanResult")
                if (scanResult == Scanning) {
                    textHold.text = getString(string.qr_code_scanning)
                    howToUseScannerHint.gone()
                } else {
                    QrCodeScanResultActivity.start(this, scanResult)
                }
            }
        )
        closeButton.setOnClickListener { finish() }
        textMoreInfo.setOnClickListener {
            startActivity<QrCodeHelpActivity>()
        }
    }

    override fun onResume() {
        super.onResume()
        textHold.text = getString(string.how_to_scan_qr_code)
        howToUseScannerHint.visible()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                resumeCamera()
            } else {
                QrCodeScanResultActivity.start(
                    this,
                    QrCodeScanResult.CameraPermissionNotGranted
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!hasCameraPermission()) {
            scannerSurfaceView.visibility = View.INVISIBLE
            requestCameraPermission()
        } else {
            resumeCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        scannerSurfaceView.stop()
        barcodeDetector?.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        scannerSurfaceView.release()
    }

    private fun resumeCamera() {
        barcodeDetector = Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        val detector = barcodeDetector ?: return

        if (!detector.isOperational) {
            if (BuildConfig.DEBUG) {
                return
            }
            Timber.e("Detector is not operational")
            finish()
            QrCodeScanResultActivity.start(
                this,
                QrCodeScanResult.ScanningNotSupported
            )
        }

        cameraSource = CameraSource.Builder(this, detector)
            .setAutoFocusEnabled(true)
            .build()

        val cameraSource = cameraSource ?: return
        scannerSurfaceView.start(cameraSource)
        scannerSurfaceView.visibility = View.VISIBLE

        detector.setProcessor(
            object : Processor<Barcode> {
                override fun release() {}
                override fun receiveDetections(detections: Detections<Barcode>) {
                    val qrCodes = detections.detectedItems
                    if (qrCodes.isEmpty()) {
                        return
                    }
                    qrCodes.forEach { _, qrCode ->
                        if (qrCode != null) {
                            viewModel.parseQrCode(qrCode.rawValue)
                            barcodeDetector?.release()
                            return@forEach
                        }
                    }
                }
            })
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 2

        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, QrScannerActivity::class.java)
    }
}
