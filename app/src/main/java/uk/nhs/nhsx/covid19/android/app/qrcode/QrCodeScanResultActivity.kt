package uk.nhs.nhsx.covid19.android.app.qrcode

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityQrCodeScanResultBinding
import uk.nhs.nhsx.covid19.android.app.permissions.PermissionsManager
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueCheckInViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.StatusActivityAction.StartInAppReview
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.ListenableAnimationDrawable
import uk.nhs.nhsx.covid19.android.app.util.viewutils.animationsDisabled
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDateTime
import javax.inject.Inject

class QrCodeScanResultActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<VenueCheckInViewModel>

    @Inject
    lateinit var permissionsManager: PermissionsManager

    private val viewModel: VenueCheckInViewModel by viewModels { factory }

    private lateinit var binding: ActivityQrCodeScanResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityQrCodeScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.getVisitRemovedResult().observe(this) {
            finish()
        }

        viewModel.isViewStateCameraPermissionNotGranted().observe(this) {
            if (permissionsManager.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED) {
                finish()
            }
        }

        viewModel.viewState().observe(this) { viewState ->
            when (viewState) {
                is ViewState.Success -> handleSuccess(
                    viewState.venueName,
                    viewState.currentDateTime,
                    viewState.playAnimation
                )
                ViewState.CameraPermissionNotGranted -> handleCameraPermissionNotGrantedState()
                ViewState.InvalidContent -> handleInvalidContentState()
                ViewState.ScanningNotSupported -> handleScanningNotSupportedState()
            }
        }

        viewModel.onCreate(intent.getParcelableExtra(SCAN_RESULT)!!)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun setTitleForAccessibility(@StringRes id: Int) {
        binding.titleTextView.setText(id)
        setTitle(id)
    }

    private fun handleSuccess(venueName: String, currentDateTime: LocalDateTime, playAnimation: Boolean) =
        with(binding) {
            animationIcon.visible()
            errorResultIcon.gone()
            if (playAnimation && !animationsDisabled(this@QrCodeScanResultActivity)) {
                val animation =
                    ContextCompat.getDrawable(baseContext, R.drawable.check_in_success_animation) as AnimationDrawable
                val listenableAnimation = ListenableAnimationDrawable(animation) {
                    viewModel.onAnimationCompleted()
                }

                animationIcon.setImageDrawable(listenableAnimation)
                listenableAnimation.isOneShot = true
                listenableAnimation.start()
            } else {
                animationIcon.setImageResource(R.drawable.tick_final_2083)
            }

            val titleText = getString(R.string.qr_code_success_title_and_venue_name, venueName)
            val spannable = SpannableString(titleText)
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                titleText.indexOf(venueName),
                titleText.indexOf(venueName) + venueName.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            titleTextView.text = spannable
            title = titleText
            successVenueDateTime.text = currentDateTime.uiFormat(this@QrCodeScanResultActivity)
            subtitleTextView.setText(R.string.qr_code_success_subtitle)
            actionButton.setText(R.string.back_to_home)
            actionButton.setOnSingleClickListener {
                StatusActivity.start(
                    this@QrCodeScanResultActivity,
                    statusActivityAction = StartInAppReview
                )
            }
            buttonCancelCheckIn.visible()
            buttonCancelCheckIn.setOnSingleClickListener {
                viewModel.removeLastVisit()
            }
            topCloseButton.gone()
            successVenueDateTime.visible()
            qrCodeHelpContainer.gone()
            qrScanHelpLink.gone()
        }

    private fun handleCameraPermissionNotGrantedState() = with(binding) {
        animationIcon.gone()
        errorResultIcon.visible()
        errorResultIcon.setImageResource(R.drawable.ic_camera)
        setTitleForAccessibility(R.string.qr_code_permission_denied_title)
        subtitleTextView.setText(R.string.qr_code_permission_denied_subtitle)
        actionButton.setText(R.string.qr_code_permission_denied_action)
        actionButton.setOnSingleClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        buttonCancelCheckIn.gone()
        topCloseButton.visible()
        topCloseButton.setOnSingleClickListener {
            StatusActivity.start(this@QrCodeScanResultActivity)
        }

        onBackPressedDispatcher.addCallback {
            StatusActivity.start(this@QrCodeScanResultActivity)
        }
        successVenueDateTime.gone()
        qrCodeHelpContainer.gone()
        qrScanHelpLink.gone()
    }

    private fun handleInvalidContentState() = with(binding) {
        qrScanHelpLink.visible()
        animationIcon.gone()
        errorResultIcon.visible()
        errorResultIcon.setImageResource(R.drawable.ic_qr_code_failure)
        setTitleForAccessibility(R.string.qr_code_failure_title)
        subtitleTextView.setText(R.string.qr_code_failure_subtitle)
        actionButton.setText(R.string.back_to_home)
        actionButton.setOnSingleClickListener {
            StatusActivity.start(this@QrCodeScanResultActivity)
        }
        qrScanHelpLink.setOnSingleClickListener {
            startActivity<QrCodeHelpActivity>()
        }
        buttonCancelCheckIn.gone()
        topCloseButton.gone()
        successVenueDateTime.gone()
        qrCodeHelpContainer.visible()
    }

    private fun handleScanningNotSupportedState() = with(binding) {
        animationIcon.gone()
        errorResultIcon.visible()
        errorResultIcon.setImageResource(R.drawable.ic_qr_code_failure)
        setTitleForAccessibility(R.string.qr_code_unsupported_title)
        subtitleTextView.setText(R.string.qr_code_unsupported_description)
        actionButton.setText(R.string.back_to_home)
        actionButton.setOnSingleClickListener {
            StatusActivity.start(this@QrCodeScanResultActivity)
        }
        buttonCancelCheckIn.gone()
        topCloseButton.gone()
        successVenueDateTime.gone()
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
