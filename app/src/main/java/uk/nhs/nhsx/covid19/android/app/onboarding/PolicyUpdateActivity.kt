package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityPolicyUpdateBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class PolicyUpdateActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<PolicyUpdateViewModel>

    private val viewModel: PolicyUpdateViewModel by viewModels { factory }

    private lateinit var binding: ActivityPolicyUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityPolicyUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {

            changedPolicyParagraph.setRawText(getString(R.string.updated_privacy_control_text))

            policyUpdateContinueButton.setOnSingleClickListener {
                viewModel.markPolicyAccepted()
                finish()
                StatusActivity.start(this@PolicyUpdateActivity)
            }
        }
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, PolicyUpdateActivity::class.java)
    }
}
