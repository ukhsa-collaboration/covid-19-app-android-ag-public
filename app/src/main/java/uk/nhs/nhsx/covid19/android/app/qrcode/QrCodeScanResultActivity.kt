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
import androidx.annotation.StringRes
import androidx.lifecycle.observe
import java.time.LocalDateTime
import javax.inject.Inject
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
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueCheckInViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class QrCodeScanResultActivity : BaseActivity(R.layout.activity_qr_code_scan_result) {

    @Inject
    lateinit var factory: ViewModelFactory<VenueCheckInViewModel>

    private val viewModel: VenueCheckInViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        viewModel.getVisitRemovedResult().observe(this) {
            finish()
        }

        viewModel.isViewStateCameraPermissionNotGranted().observe(this) {
            if (checkSelfPermission(CAMERA) == PERMISSION_GRANTED) {
                finish()
            }
        }

        viewModel.viewState().observe(this) { viewState ->
            when (viewState) {
                is ViewState.Success -> handleSuccess(viewState.venueName, viewState.currentDateTime)
                ViewState.CameraPermissionNotGranted -> handleCameraPermissionNotGrantedState()
                ViewState.InvalidContent -> handleInvalidContentState()
                ViewState.ScanningNotSupported -> handleScanningNotSupportedState()
            }
        }

        viewModel.onCreate(intent.getParcelableExtra(SCAN_RESULT) as QrCodeScanResult)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setTitleForAccessibility(@StringRes id: Int) {
        titleTextView.setText(id)
        setTitle(id)
    }

    private fun handleSuccess(venueName: String, currentDateTime: LocalDateTime) {
        resultIcon.setImageResource(R.drawable.ic_qr_code_success)
        setTitleForAccessibility(R.string.qr_code_success_title)
        successVenueName.text = venueName
        successVenueDateTime.text = currentDateTime.uiFormat(this@QrCodeScanResultActivity)
        subtitleTextView.setText(R.string.qr_code_success_subtitle)
        actionButton.setText(R.string.back_to_home)
        actionButton.setOnSingleClickListener {
            StatusActivity.start(
                this@QrCodeScanResultActivity,
                startedFromVenueCheckInSuccess = true
            )
        }
        qrScanHelpLink.setOnSingleClickListener {
            startActivity<QrCodeHelpActivity>()
        }
        textCancelCheckIn.visible()
        textCancelCheckIn.setOnSingleClickListener {
            viewModel.removeLastVisit()
        }
        topCloseButton.gone()
        venueInfoContainer.visible()
        qrCodeHelpContainer.gone()
        qrScanHelpLink.visible()
    }

    private fun handleCameraPermissionNotGrantedState() {
        resultIcon.setImageResource(R.drawable.ic_camera)
        setTitleForAccessibility(R.string.qr_code_permission_denied_title)
        subtitleTextView.setText(R.string.qr_code_permission_denied_subtitle)
        actionButton.setText(R.string.qr_code_permission_denied_action)
        actionButton.setOnSingleClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        textCancelCheckIn.gone()
        topCloseButton.visible()
        topCloseButton.setOnSingleClickListener {
            StatusActivity.start(this@QrCodeScanResultActivity)
        }

        onBackPressedDispatcher.addCallback {
            StatusActivity.start(this@QrCodeScanResultActivity)
        }
        venueInfoContainer.gone()
        qrCodeHelpContainer.gone()
        qrScanHelpLink.gone()
    }

    private fun handleInvalidContentState() {
        qrScanHelpLink.visible()
        resultIcon.setImageResource(R.drawable.ic_qr_code_failure)
        setTitleForAccessibility(R.string.qr_code_failure_title)
        subtitleTextView.setText(R.string.qr_code_failure_subtitle)
        actionButton.setText(R.string.back_to_home)
        actionButton.setOnSingleClickListener {
            StatusActivity.start(this@QrCodeScanResultActivity)
        }
        qrScanHelpLink.setOnSingleClickListener {
            startActivity<QrCodeHelpActivity>()
        }
        textCancelCheckIn.gone()
        topCloseButton.gone()
        venueInfoContainer.gone()
        qrCodeHelpContainer.visible()
    }

    private fun handleScanningNotSupportedState() {
        resultIcon.setImageResource(R.drawable.ic_qr_code_failure)
        setTitleForAccessibility(R.string.qr_code_unsupported_title)
        subtitleTextView.setText(R.string.qr_code_unsupported_description)
        actionButton.setText(R.string.back_to_home)
        actionButton.setOnSingleClickListener {
            StatusActivity.start(this@QrCodeScanResultActivity)
        }
        textCancelCheckIn.gone()
        topCloseButton.gone()
        venueInfoContainer.gone()
        qrCodeHelpContainer.gone()
        qrScanHelpLink.gone()
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
