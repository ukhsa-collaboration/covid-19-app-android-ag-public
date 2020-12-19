package uk.nhs.nhsx.covid19.android.app.exposure

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_share_keys_information.shareKeysConfirm
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Error
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Failure
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.FetchTemporaryExposureKeys.TemporaryExposureKeysFetchResult.Success
import uk.nhs.nhsx.covid19.android.app.status.ExposureStatusViewModel
import uk.nhs.nhsx.covid19.android.app.status.ExposureStatusViewModel.Companion.REQUEST_CODE_SUBMIT_KEYS_PERMISSION
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity.Companion.REQUEST_CODE_START_EXPOSURE_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitKeysProgressActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ShareKeysInformationActivity : BaseActivity(R.layout.activity_share_keys_information) {

    @Inject
    lateinit var shareKeysInformationFactory: ViewModelFactory<ShareKeysInformationViewModel>
    private val shareKeysInformationViewModel: ShareKeysInformationViewModel by viewModels { shareKeysInformationFactory }

    @Inject
    lateinit var exposureStatusViewModelFactory: ViewModelFactory<ExposureStatusViewModel>
    private val exposureStatusViewModel: ExposureStatusViewModel by viewModels { exposureStatusViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar,
            R.string.submit_keys_information_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        intent.getParcelableExtra<ReceivedTestResult>(EXTRA_TEST_RESULT)?.let {
            shareKeysInformationViewModel.testResult = it

            shareKeysConfirm.setOnSingleClickListener {
                shareKeysInformationViewModel.fetchKeys()
            }
            setupViewModelListeners()
        } ?: finish()
    }

    private fun setupViewModelListeners() {
        shareKeysInformationViewModel.fetchKeysResult().observe(
            this,
            Observer { result ->
                when (result) {
                    is Success -> {
                        disableExposureNotificationsAgainIfWasInitiallyDisabled()
                        SubmitKeysProgressActivity.startForResult(
                            this,
                            result.temporaryExposureKeys,
                            shareKeysInformationViewModel.testResult.diagnosisKeySubmissionToken,
                            REQUEST_CODE_SUBMIT_KEYS
                        )
                    }
                    is Failure -> {
                        val exposureNotificationsDisabled =
                            result.throwable is ApiException && result.throwable.statusCode == ConnectionResult.DEVELOPER_ERROR

                        if (exposureNotificationsDisabled) {
                            exposureStatusViewModel.startExposureNotifications()
                        } else {
                            Timber.e(result.throwable, "Failed to fetch exposure keys")
                            finish()
                        }
                    }
                    is ResolutionRequired -> handleSubmitKeysResolution(result.status)
                }
            }
        )

        exposureStatusViewModel.exposureNotificationsChanged().observe(this) { isEnabled ->
            if (isEnabled && !shareKeysInformationViewModel.handleSubmitKeyResolutionStarted) {
                shareKeysInformationViewModel.fetchKeys()
            } else if (!isEnabled) {
                shareKeysInformationViewModel.exposureNotificationWasInitiallyDisabled = true
            }
        }

        exposureStatusViewModel.exposureNotificationActivationResult().observe(this) { viewState ->
            when (viewState) {
                ExposureNotificationActivationResult.Success ->
                    Timber.d("Exposure notifications successfully started")
                is Error ->
                    Timber.e(viewState.exception, "Could not start exposure notifications")
                is ExposureNotificationActivationResult.ResolutionRequired ->
                    handleEnableExposureNotificationsResolution(viewState.status)
            }
        }
    }

    private fun handleEnableExposureNotificationsResolution(status: Status) {
        status.startResolutionForResult(this, REQUEST_CODE_START_EXPOSURE_NOTIFICATION)
    }

    private fun handleSubmitKeysResolution(status: Status) {
        shareKeysInformationViewModel.handleSubmitKeyResolutionStarted = true
        status.startResolutionForResult(this, REQUEST_CODE_SUBMIT_KEYS_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SUBMIT_KEYS_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                shareKeysInformationViewModel.fetchKeys()
            } else {
                shareKeysInformationViewModel.onSubmitKeysDenied()
                disableExposureNotificationsAgainIfWasInitiallyDisabled()
                StatusActivity.start(this)
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION) {
            exposureStatusViewModel.startExposureNotifications()
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SUBMIT_KEYS) {
            shareKeysInformationViewModel.onSubmitKeysSuccess()
            disableExposureNotificationsAgainIfWasInitiallyDisabled()
            StatusActivity.start(this)
        }
    }

    private fun disableExposureNotificationsAgainIfWasInitiallyDisabled() {
        if (shareKeysInformationViewModel.exposureNotificationWasInitiallyDisabled) {
            exposureStatusViewModel.stopExposureNotifications()
        }
    }

    companion object {
        private const val EXTRA_TEST_RESULT = "EXTRA_TEST_RESULT"
        private const val REQUEST_CODE_SUBMIT_KEYS = 1403

        fun start(
            context: Context,
            testResult: ReceivedTestResult
        ) =
            context.startActivity(getIntent(context, testResult))

        private fun getIntent(
            context: Context,
            testResult: ReceivedTestResult
        ) =
            Intent(context, ShareKeysInformationActivity::class.java)
                .putExtra(
                    EXTRA_TEST_RESULT,
                    testResult
                )
    }
}
