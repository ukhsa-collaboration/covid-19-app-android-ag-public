package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySymptomsAfterRiskyVenueBinding
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Home
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.OrderLfdTest
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.NavigationTarget.Questionnaire
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireActivity
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.lfd.OrderLfdTestActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCancelToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setMultilineTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class SymptomsAfterRiskyVenueActivity : BaseActivity() {

    private lateinit var binding: ActivitySymptomsAfterRiskyVenueBinding
    private var cancelDialog: AlertDialog? = null

    @Inject
    lateinit var factory: SymptomsAfterRiskyVenueViewModel.Factory
    private val viewModel: SymptomsAfterRiskyVenueViewModel by assistedViewModel {
        factory.create(
            intent.getBooleanExtra(
                EXTRA_SHOULD_SHOW_CANCEL_CONFIRMATION_DIALOG_ON_CANCEL_BUTTON_CLICK, false
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySymptomsAfterRiskyVenueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setCancelToolbar(binding.primaryToolbar.toolbar, R.string.symptoms_after_risky_venue_title)

        setupViewModelListeners()

        setupOnClickListeners()
    }

    override fun onDestroy() {
        cancelDialog?.setOnDismissListener { }
        // To avoid leaking the window
        cancelDialog?.dismiss()
        cancelDialog = null

        super.onDestroy()
    }

    override fun onBackPressed() {
        viewModel.onCancelButtonClicked()
    }

    private fun setupViewModelListeners() {
        viewModel.viewState().observe(this) { viewState ->
            if (viewState.showCancelDialog) {
                showCancelDialog()
            } else {
                cancelDialog?.dismiss()
                cancelDialog = null
            }
        }

        viewModel.navigationTarget().observe(this) {
            when (it) {
                Questionnaire -> startActivity<QuestionnaireActivity>()
                OrderLfdTest -> startActivity<OrderLfdTestActivity>()
                Home -> StatusActivity.start(this)
                Finish -> finish()
            }
        }
    }

    private fun setupOnClickListeners() = with(binding) {
        hasSymptomsButton.setOnSingleClickListener(viewModel::onHasSymptomsClicked)
        noSymptomsButton.setOnSingleClickListener(viewModel::onHasNoSymptomsClicked)
    }

    private fun showCancelDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
            .setMultilineTitle(getString(R.string.symptoms_after_risky_venue_cancel_dialog_title))
            .setMessage(R.string.symptoms_after_risky_venue_cancel_dialog_text)
            .setPositiveButton(R.string.symptoms_after_risky_venue_cancel_dialog_positive) { _, _ ->
                viewModel.onDialogOptionLeaveClicked()
            }
            .setNegativeButton(R.string.symptoms_after_risky_venue_cancel_dialog_negative) { _, _ ->
                viewModel.onDialogOptionStayClicked()
            }

        cancelDialog = dialogBuilder.show()
    }

    companion object {
        private const val EXTRA_SHOULD_SHOW_CANCEL_CONFIRMATION_DIALOG_ON_CANCEL_BUTTON_CLICK =
            "EXTRA_SHOULD_SHOW_CANCEL_CONFIRMATION_DIALOG_ON_CANCEL_BUTTON_CLICK"

        fun start(context: Context, shouldShowCancelConfirmationDialogOnCancelButtonClick: Boolean = false) {
            context.startActivity(getIntent(context, shouldShowCancelConfirmationDialogOnCancelButtonClick))
        }

        private fun getIntent(context: Context, shouldShowCancelConfirmationDialogOnCancelButtonClick: Boolean) =
            Intent(context, SymptomsAfterRiskyVenueActivity::class.java)
                .apply {
                    putExtra(
                        EXTRA_SHOULD_SHOW_CANCEL_CONFIRMATION_DIALOG_ON_CANCEL_BUTTON_CLICK,
                        shouldShowCancelConfirmationDialogOnCancelButtonClick
                    )
                }
    }
}
