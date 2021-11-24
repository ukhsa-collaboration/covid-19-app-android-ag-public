package uk.nhs.nhsx.covid19.android.app.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsViewModel.ActivationResult.Error
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsViewModel.ActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityEdgeCaseBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class EnableExposureNotificationsActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<EnableExposureNotificationsViewModel>
    private val viewModel: EnableExposureNotificationsViewModel by viewModels { factory }

    private lateinit var binding: ActivityEdgeCaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityEdgeCaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            edgeCaseTitle.text = getString(R.string.enable_exposure_notifications_title)
            edgeCaseText.text = getString(R.string.enable_exposure_notifications_rationale)
            takeActionButton.text = getString(R.string.enable_exposure_notifications)

            startListeningToViewState()

            takeActionButton.setOnSingleClickListener {
                viewModel.onEnableExposureNotificationsClicked()
            }
        }
    }

    private fun startListeningToViewState() {
        viewModel.permissionRequest().observe(this) { permissionRequest ->
            permissionRequest(this)
        }

        viewModel.activationResult().observe(this) { activationResult ->
            when (activationResult) {
                Success -> handleSuccess()
                is Error -> handleError(activationResult.message)
            }
        }
    }

    private fun handleSuccess() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun handleError(message: String) {
        Snackbar.make(binding.edgeCaseContainer, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode)
    }
}
