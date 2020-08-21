package uk.nhs.nhsx.covid19.android.app.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_permission.permissionContinue
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.ExposureStatusViewModel
import javax.inject.Inject

class PermissionActivity : BaseActivity(R.layout.activity_permission) {

    @Inject
    lateinit var factory: ViewModelFactory<ExposureStatusViewModel>

    private val viewModel: ExposureStatusViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        permissionContinue.setOnClickListener {
            viewModel.startExposureNotifications()
        }

        viewModel.exposureNotificationActivationResult().observe(
            this,
            Observer { viewState ->
                when (viewState) {
                    is ExposureNotificationActivationResult.ResolutionRequired ->
                        handleResolution(viewState.status)
                    ExposureNotificationActivationResult.Success ->
                        PostCodeActivity.start(this)
                    is ExposureNotificationActivationResult.Error ->
                        handleError(viewState.exception)
                }
            }
        )
    }

    private fun handleError(exception: Exception) {
        startActivity<DeviceNotSupportedActivity>()
    }

    private fun handleResolution(status: Status) {
        status.startResolutionForResult(
            this,
            REQUEST_CODE_START_EXPOSURE_NOTIFICATION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.startExposureNotifications()
            } else {
                val intent = Intent(this, EnableExposureNotificationsActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_START_EXPOSURE_NOTIFICATION_RATIONALE)
            }
        } else if (requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION_RATIONALE &&
            resultCode == Activity.RESULT_OK
        ) {
            viewModel.startExposureNotifications()
        }
    }

    companion object {
        const val REQUEST_CODE_START_EXPOSURE_NOTIFICATION = 1337
        const val REQUEST_CODE_START_EXPOSURE_NOTIFICATION_RATIONALE = 1338

        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, PermissionActivity::class.java)
    }
}
