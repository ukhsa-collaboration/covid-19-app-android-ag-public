package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_contact_tracing_hub.contactTracingHubContainer
import kotlinx.android.synthetic.main.activity_contact_tracing_hub.contactTracingStatus
import kotlinx.android.synthetic.main.activity_contact_tracing_hub.encounterDetectionSwitch
import kotlinx.android.synthetic.main.activity_contact_tracing_hub.optionContactTracing
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.ExposureNotificationReminderDialog
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class ContactTracingHubActivity : BaseActivity(R.layout.activity_contact_tracing_hub) {

    @Inject
    lateinit var factory: ViewModelFactory<ContactTracingHubViewModel>
    private val viewModel: ContactTracingHubViewModel by viewModels { factory }

    /**
     * Exposure notification dialog currently displayed, or null if none are displayed
     */
    private var currentExposureNotificationReminderDialog: ExposureNotificationReminderDialog? =
        null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startListeningToViewState()

        setNavigateUpToolbar(toolbar, titleResId = R.string.contact_tracing_hub_title)

        optionContactTracing.setOnSingleClickListener {
            viewModel.onContactTracingToggleClicked()
        }

        val shouldTurnOnContactTracing = intent.getBooleanExtra(SHOULD_TURN_ON_CONTACT_TRACING, false)
        if (shouldTurnOnContactTracing) {
            viewModel.onTurnOnContactTracingExtraReceived()
        }
    }

    private fun startListeningToViewState() {
        viewModel.permissionRequest().observe(this) { result ->
            when (result) {
                is Request -> result.callback(this)
                is Error -> Snackbar.make(contactTracingHubContainer, result.message, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.viewState.observe(this) { viewState ->
            handleExposureNotificationState(viewState.exposureNotificationEnabled)
            handleReminderDialogState(viewState.showReminderDialog)
        }
    }

    private fun handleExposureNotificationState(exposureNotificationEnabled: Boolean) {
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
