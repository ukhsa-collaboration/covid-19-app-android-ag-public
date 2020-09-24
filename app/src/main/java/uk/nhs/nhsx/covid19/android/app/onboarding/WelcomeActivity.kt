package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_welcome.confirmOnboarding
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.edgecases.AgeRestrictionActivity

class WelcomeActivity : BaseActivity(R.layout.activity_welcome) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        confirmOnboarding.setOnClickListener {
            showAgeConfirmationDialog()
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

        val alertDialog = builder.show()

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.isSingleLine = false
        positiveButton.isAllCaps = true
        positiveButton.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
        positiveButton.maxLines = 3
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, WelcomeActivity::class.java)
    }
}
