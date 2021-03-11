package uk.nhs.nhsx.covid19.android.app.onboarding

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_welcome.confirmOnboarding
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.edgecases.AgeRestrictionActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class WelcomeActivity : BaseActivity(R.layout.activity_welcome) {

    @Inject
    lateinit var factory: ViewModelFactory<WelcomeViewModel>

    private val viewModel: WelcomeViewModel by viewModels { factory }

    /**
     * Dialog currently displayed, or null if none are displayed
     */
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appComponent.inject(this)

        viewModel.getShowDialog().observe(this) { showDialog ->
            if (showDialog) {
                showAgeConfirmationDialog()
            }
        }

        confirmOnboarding.setOnSingleClickListener {
            viewModel.onConfirmOnboardingClicked()
        }
    }

    private fun showAgeConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.onboarding_age_confirmation_title))
        builder.setMessage(R.string.onboarding_age_confirmation_text)
        builder.setPositiveButton(
            R.string.onboarding_age_confirmation_positive
        ) { _, _ ->
            DataAndPrivacyActivity.start(this)
        }

        builder.setNegativeButton(
            R.string.onboarding_age_confirmation_negative
        ) { dialog, _ ->
            dialog.dismiss()
            AgeRestrictionActivity.start(this)
            finish()
        }

        builder.setOnDismissListener {
            currentDialog = null
            viewModel.onDialogDismissed()
        }

        val alertDialog = builder.show()
        currentDialog = alertDialog

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isSingleLine = false
        positiveButton.isAllCaps = true
        positiveButton.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
        positiveButton.maxLines = 3
    }

    override fun onDestroy() {
        currentDialog?.setOnDismissListener { }
        // To avoid leaking the window
        currentDialog?.dismiss()
        currentDialog = null

        super.onDestroy()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, WelcomeActivity::class.java)
    }
}
