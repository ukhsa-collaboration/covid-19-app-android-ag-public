package uk.nhs.nhsx.covid19.android.app.qrcode

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.observe
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.actionButton
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.qrCodeHelpContainer
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.qrScanHelpLink
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.resultIcon
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.subtitleTextView
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.successVenueDateTime
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.successVenueName
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.textCancelCheckIn
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.titleTextView
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.topCloseButton
import kotlinx.android.synthetic.main.activity_qr_code_scan_result.venueInfoContainer
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.CameraPermissionNotGranted
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.InvalidContent
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Scanning
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.ScanningNotSupported
import uk.nhs.nhsx.covid19.android.app.qrcode.QrCodeScanResult.Success
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDateTime
import javax.inject.Inject

class QrCodeScanResultActivity : BaseActivity(R.layout.activity_qr_code_scan_result) {

    private lateinit var state: State

    @Inject
    lateinit var factory: ViewModelFactory<VenueCheckInViewModel>

    private val viewModel: VenueCheckInViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        viewModel.getVisitRemovedResult().observe(this) {
            finish()
        }

        state = when (val scanResult = intent.getParcelableExtra(SCAN_RESULT) as QrCodeScanResult) {
            is Success -> SuccessState(scanResult.venueName)
            CameraPermissionNotGranted -> CameraPermissionNotGrantedState()
            InvalidContent -> InvalidContentState()
            ScanningNotSupported -> ScanningNotSupportedState()
            Scanning -> InvalidContentState()
        }
        state.setup()
    }

    override fun onResume() {
        super.onResume()
        state.onResume()
    }

    interface State {
        fun setup()
        fun onResume() {}
    }

    inner class SuccessState(private val venueName: String) : State {
        override fun setup() {
            val currentDateTime = LocalDateTime.now()
            resultIcon.setImageResource(R.drawable.ic_qr_code_success)
            titleTextView.setText(R.string.qr_code_success_title)
            successVenueName.text = venueName
            successVenueDateTime.text = currentDateTime.uiFormat(this@QrCodeScanResultActivity)
            subtitleTextView.setText(R.string.qr_code_success_subtitle)
            actionButton.setText(R.string.back_to_home)
            actionButton.setOnClickListener {
                StatusActivity.start(
                    this@QrCodeScanResultActivity,
                    startedFromVenueCheckInSuccess = true
                )
            }
            textCancelCheckIn.visible()
            textCancelCheckIn.setOnClickListener {
                viewModel.removeLastVisit()
            }
            venueInfoContainer.visible()
            qrCodeHelpContainer.gone()
        }
    }

    inner class CameraPermissionNotGrantedState : State {
        override fun setup() {
            resultIcon.setImageResource(R.drawable.ic_camera)
            titleTextView.setText(R.string.qr_code_permission_denied_title)
            subtitleTextView.setText(R.string.qr_code_permission_denied_subtitle)
            actionButton.setText(R.string.qr_code_permission_denied_action)
            actionButton.setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            textCancelCheckIn.gone()
            topCloseButton.visible()
            topCloseButton.setOnClickListener {
                StatusActivity.start(this@QrCodeScanResultActivity)
            }

            onBackPressedDispatcher.addCallback {
                StatusActivity.start(this@QrCodeScanResultActivity)
            }
            venueInfoContainer.gone()
            qrCodeHelpContainer.gone()
        }

        override fun onResume() {
            if (checkSelfPermission(CAMERA) == PERMISSION_GRANTED) {
                finish()
            }
        }
    }

    inner class InvalidContentState : State {
        override fun setup() {
            resultIcon.setImageResource(R.drawable.ic_qr_code_failure)
            titleTextView.setText(R.string.qr_code_failure_title)
            subtitleTextView.setText(R.string.qr_code_failure_subtitle)
            actionButton.setText(R.string.back_to_home)
            actionButton.setOnClickListener {
                StatusActivity.start(this@QrCodeScanResultActivity)
            }
            qrScanHelpLink.setOnClickListener {
                startActivity<QrCodeHelpActivity>()
            }
            textCancelCheckIn.gone()
            topCloseButton.gone()
            venueInfoContainer.gone()
            qrCodeHelpContainer.visible()
        }
    }

    inner class ScanningNotSupportedState : State {
        override fun setup() {
            resultIcon.setImageResource(R.drawable.ic_qr_code_failure)
            titleTextView.setText(R.string.qr_code_unsupported_title)
            subtitleTextView.setText(R.string.qr_code_unsupported_description)
            actionButton.setText(R.string.back_to_home)
            actionButton.setOnClickListener {
                StatusActivity.start(this@QrCodeScanResultActivity)
            }
            textCancelCheckIn.gone()
            topCloseButton.gone()
            venueInfoContainer.gone()
            qrCodeHelpContainer.gone()
        }
    }

    companion object {

        const val SCAN_RESULT = "SCAN_RESULT"

        fun start(context: Context, qrCodeScanResult: QrCodeScanResult) =
            context.startActivity(getIntent(context, qrCodeScanResult))

        private fun getIntent(context: Context, qrCodeScanResult: QrCodeScanResult) =
            Intent(context, QrCodeScanResultActivity::class.java).putExtra(
                SCAN_RESULT,
                qrCodeScanResult
            )
    }
}
