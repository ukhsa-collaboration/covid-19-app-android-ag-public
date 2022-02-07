package uk.nhs.nhsx.covid19.android.app.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationActivity
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityOnboardingPermissionBluetoothBinding
import uk.nhs.nhsx.covid19.android.app.edgecases.DeviceNotSupportedActivity
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.BatteryOptimization
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.EnableExposureNotifications
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.Status
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class PermissionActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<PermissionViewModel>
    private val viewModel: PermissionViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        val binding = ActivityOnboardingPermissionBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.permissionContinue.setOnSingleClickListener {
            viewModel.onContinueButtonClicked()
        }

        viewModel.permissionRequest().observe(this) { result ->
            when (result) {
                is Request -> result.callback(this)
                is PermissionRequestResult.Error -> handleError(result.message)
            }
        }

        viewModel.navigationTarget().observe(this) { navigationTarget ->
            when (navigationTarget) {
                BatteryOptimization -> startActivity<BatteryOptimizationActivity>()
                Status -> StatusActivity.start(this)
                EnableExposureNotifications -> startEnableExposureNotificationsActivity()
            }
        }
    }

    private fun handleError(message: String) {
        Timber.d(message)
        startActivity<DeviceNotSupportedActivity>()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode)

        if (requestCode == REQUEST_CODE_START_EXPOSURE_NOTIFICATION_RATIONALE && resultCode == Activity.RESULT_OK) {
            viewModel.onExposureNotificationsEnabled()
        }
    }

    private fun startEnableExposureNotificationsActivity() {
        val intent = Intent(this, EnableExposureNotificationsActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_START_EXPOSURE_NOTIFICATION_RATIONALE)
    }

    companion object {
        const val REQUEST_CODE_START_EXPOSURE_NOTIFICATION_RATIONALE = 1338
    }
}
