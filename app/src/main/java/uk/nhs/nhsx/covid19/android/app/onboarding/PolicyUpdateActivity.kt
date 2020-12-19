package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import kotlinx.android.synthetic.main.activity_policy_update.changedPolicyParagraph
import kotlinx.android.synthetic.main.activity_policy_update.policyUpdateContinueButton
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.widgets.setRawText
import javax.inject.Inject

class PolicyUpdateActivity : BaseActivity(R.layout.activity_policy_update) {

    @Inject
    lateinit var factory: ViewModelFactory<PolicyUpdateViewModel>

    private val viewModel: PolicyUpdateViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        changedPolicyParagraph.setRawText(getString(R.string.updated_privacy_control_text))

        policyUpdateContinueButton.setOnSingleClickListener {
            viewModel.markPolicyAccepted()
            finish()
            StatusActivity.start(this)
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, PolicyUpdateActivity::class.java)
    }
}
