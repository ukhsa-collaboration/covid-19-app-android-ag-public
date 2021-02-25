package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import android.app.Dialog
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_daily_contact_testing_confirmation.confirmDailyContactTesting
import kotlinx.android.synthetic.main.view_toolbar_primary.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class DailyContactTestingConfirmationActivity : BaseActivity(R.layout.activity_daily_contact_testing_confirmation) {

    @Inject
    lateinit var factory: ViewModelFactory<DailyContactTestingConfirmationViewModel>

    private val viewModel: DailyContactTestingConfirmationViewModel by viewModels { factory }

    /**
     * Dialog currently displayed, or null if none are displayed
     */
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        setNavigateUpToolbar(
            toolbar,
            R.string.daily_contact_testing_confirmation_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        confirmDailyContactTesting.setOnSingleClickListener {
            viewModel.onOpenDialogClicked()
        }

        startViewModelListeners()
    }

    private fun startViewModelListeners() {
        viewModel.showDialog().observe(this) { showDialog ->
            if (showDialog) {
                showConfirmationDialog()
            }
        }

        viewModel.confirmedDailyContactTesting().observe(this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun showConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(getString(R.string.daily_contact_testing_confirmation_dialog_title))
        dialogBuilder.setMessage(R.string.daily_contact_testing_confirmation_dialog_text)
        dialogBuilder.setPositiveButton(R.string.confirm) { _, _ ->
            viewModel.onOptInToDailyContactTestingConfirmed()
        }

        dialogBuilder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }

        dialogBuilder.setOnDismissListener {
            currentDialog = null
            viewModel.onDialogDismissed()
        }

        currentDialog = dialogBuilder.show()
    }

    override fun onDestroy() {
        currentDialog?.setOnDismissListener { }
        // To avoid leaking the window
        currentDialog?.dismiss()
        currentDialog = null

        super.onDestroy()
    }
}
