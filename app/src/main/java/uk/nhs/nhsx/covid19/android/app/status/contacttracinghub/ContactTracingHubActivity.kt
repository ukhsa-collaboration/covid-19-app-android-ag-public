package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityContactTracingHubBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.ExposureNotificationReminderDialog
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.status.contacttracinghub.ContactTracingHubViewModel.NavigationTarget.WhenNotToPauseContactTracing
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpButtonType
import javax.inject.Inject

class ContactTracingHubActivity : BaseActivity() {

    @Inject
    lateinit var factory: ContactTracingHubViewModel.Factory

    private val viewModel: ContactTracingHubViewModel by assistedViewModel {
        factory.create(intent.getBooleanExtra(SHOULD_TURN_ON_CONTACT_TRACING, false))
    }

    private lateinit var binding: ActivityContactTracingHubBinding

    /**
     * Exposure notification dialog currently displayed, or null if none are displayed
     */
    private var currentExposureNotificationReminderDialog: ExposureNotificationReminderDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityContactTracingHubBinding.inflate(layoutInflater)

        with(binding) {

            setContentView(binding.root)

            startListeningToViewState()

            setNavigateUpToolbar(primaryToolbar.toolbar, titleResId = R.string.contact_tracing_hub_title)

            optionWhenNotToPause.setUpButtonType(getString(R.string.contact_tracing_when_not_to_pause_option))
        }

        setupOnClickListeners()
        viewModel.onCreate()
    }

    private fun setupOnClickListeners() = with(binding) {
        optionContactTracing.setOnSingleClickListener {
            viewModel.onContactTracingToggleClicked()
        }

        optionWhenNotToPause.setOnSingleClickListener {
            viewModel.onWhenNotToPauseClicked()
        }
    }

    private fun startListeningToViewState() {
        viewModel.permissionRequest().observe(this) { result ->
            when (result) {
                is Request -> result.callback(this)
                is Error ->
                    Snackbar.make(binding.contactTracingHubContainer, result.message, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.navigationTarget().observe(this) { navigationTarget ->
            when (navigationTarget) {
                WhenNotToPauseContactTracing -> startActivity<WhenNotToPauseContactTracingActivity>()
            }
        }

        viewModel.viewState.observe(this) { viewState ->
            handleExposureNotificationState(viewState.exposureNotificationEnabled)
            handleReminderDialogState(viewState.showReminderDialog)
        }
    }

    private fun handleExposureNotificationState(exposureNotificationEnabled: Boolean) = with(binding) {
        encounterDetectionSwitch.isChecked = exposureNotificationEnabled
        val contactTracingStatusResId =
            if (exposureNotificationEnabled) {
                R.string.contact_tracing_hub_status_on
            } else {
                R.string.contact_tracing_hub_status_off
            }
        contactTracingStatus.text = getString(contactTracingStatusResId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode)
    }

    override fun onDestroy() {
        // To avoid leaking the window
        currentExposureNotificationReminderDialog?.setOnDismissListener { }
        dismissExposureNotificationReminderDialog()

        super.onDestroy()
    }

    private fun handleReminderDialogState(showExposureNotificationReminderDialog: Boolean) {
        if (showExposureNotificationReminderDialog) {
            showExposureNotificationReminderDialog()
        } else {
            dismissExposureNotificationReminderDialog()
        }
    }

    private fun showExposureNotificationReminderDialog() {
        val dialog = ExposureNotificationReminderDialog(this) { duration ->
            viewModel.onReminderDelaySelected(duration)
        }
        dialog.setOnDismissListener {
            viewModel.onExposureNotificationReminderDialogDismissed()
            currentExposureNotificationReminderDialog = null
        }
        currentExposureNotificationReminderDialog = dialog
        dialog.show()
    }

    private fun dismissExposureNotificationReminderDialog() {
        currentExposureNotificationReminderDialog?.dismiss()
        currentExposureNotificationReminderDialog = null
    }

    companion object {
        fun start(context: Context, shouldTurnOnContactTracing: Boolean = false) {
            context.startActivity(getIntent(context, shouldTurnOnContactTracing))
        }

        private fun getIntent(context: Context, shouldTurnOnContactTracing: Boolean) =
            Intent(context, ContactTracingHubActivity::class.java)
                .apply {
                    putExtra(SHOULD_TURN_ON_CONTACT_TRACING, shouldTurnOnContactTracing)
                }

        const val SHOULD_TURN_ON_CONTACT_TRACING = "SHOULD_TURN_ON_CONTACT_TRACING"
    }
}
