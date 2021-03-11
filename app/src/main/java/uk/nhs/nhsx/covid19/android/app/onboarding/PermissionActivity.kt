package uk.nhs.nhsx.covid19.android.app.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.Status
import kotlinx.android.synthetic.main.activity_permission.permissionContinue
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationActivity
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Error
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.BATTERY_OPTIMIZATION
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.STATUS_ACTIVITY
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.ExposureStatusViewModel
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class PermissionActivity : BaseActivity(R.layout.activity_permission) {

    @Inject
    lateinit var permissionFactory: ViewModelFactory<PermissionViewModel>
    private val permissionViewModel: PermissionViewModel by viewModels { permissionFactory }

    @Inject
    lateinit var exposureStatusViewModelFactory: ViewModelFactory<ExposureStatusViewModel>
    private val exposureStatusViewModel: ExposureStatusViewModel by viewModels { exposureStatusViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        permissionContinue.setOnSingleClickListener {
            permissionContinue.isEnabled = false
            exposureStatusViewModel.startExposureNotifications()
        }

        startObservingExposureNotificationActivation()

        permissionViewModel.onActivityNavigation().observe(this) { navigationTarget ->
            when (navigationTarget) {
                BATTERY_OPTIMIZATION -> startActivity<BatteryOptimizationActivity>()
                STATUS_ACTIVITY -> StatusActivity.start(this)
            }
        }
    }

    private fun startObservingExposureNotificationActivation() {
        exposureStatusViewModel.exposureNotificationActivationResult().observe(
            this,
            Observer { viewState ->
                when (viewState) {
                    is ResolutionRequired ->
                        handleResolution(viewState.status)
                    Success ->
                        permissionViewModel.onExposureNotificationsActive()
                    is Error ->
                        handleError(viewState.exception)
                }
                permissionContinue.isEnabled = true
            }
        )
    }

    private fun handleError(exception: Exception) {
        Timber.e(exception)
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
                exposureStatusViewModel.startExposureNotifications()
            } else {
                val intent = Intent(this, EnableExposureNotificationsActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_START_EXPOSURE_NOTIFICATION_RATIONALE)
            }
        } else if (requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION_RATIONALE &&
            resultCode == Activity.RESULT_OK
        ) {
            exposureStatusViewModel.startExposureNotifications()
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
