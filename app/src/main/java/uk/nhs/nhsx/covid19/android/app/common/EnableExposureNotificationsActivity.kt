package uk.nhs.nhsx.covid19.android.app.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.Status
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseText
import kotlinx.android.synthetic.main.activity_edge_case.edgeCaseTitle
import kotlinx.android.synthetic.main.activity_edge_case.takeActionButton
import kotlinx.android.synthetic.main.activity_status.statusContainer
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Error
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionActivity
import uk.nhs.nhsx.covid19.android.app.status.ExposureStatusViewModel
import javax.inject.Inject

class EnableExposureNotificationsActivity : BaseActivity(R.layout.activity_edge_case) {

    @Inject
    lateinit var factory: ViewModelFactory<ExposureStatusViewModel>

    private val viewModel: ExposureStatusViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        edgeCaseTitle.text = getString(R.string.enable_exposure_notifications_title)
        edgeCaseText.text = getString(R.string.enable_exposure_notifications_rationale)

        takeActionButton.text = getString(R.string.enable_exposure_notifications)

        viewModel.exposureNotificationActivationResult().observe(
            this,
            Observer { viewState ->
                when (viewState) {
                    is ResolutionRequired -> handleResolution(viewState.status)
                    Success -> handleSuccess()
                    is Error -> handleError(viewState.exception)
                }
            }
        )

        takeActionButton.setOnClickListener {
            viewModel.startExposureNotifications()
        }
    }

    private fun handleSuccess() {
        setResult(Activity.RESULT_OK)
        this@EnableExposureNotificationsActivity.finish()
    }

    private fun handleError(exception: Exception) {
        Snackbar.make(statusContainer, exception.message.toString(), Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun handleResolution(status: Status) {
        status.startResolutionForResult(
            this,
            PermissionActivity.REQUEST_CODE_START_EXPOSURE_NOTIFICATION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionActivity.REQUEST_CODE_START_EXPOSURE_NOTIFICATION &&
            resultCode == Activity.RESULT_OK
        ) {
            viewModel.startExposureNotifications()
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(
                getIntent(
                    context
                )
            )
        }

        private fun getIntent(context: Context) =
            Intent(context, EnableExposureNotificationsActivity::class.java)
    }
}
