package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityRiskyContactIsolationOptOutBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpOpensInBrowserWarning
import javax.inject.Inject

class RiskyContactIsolationOptOutActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<RiskyContactIsolationOptOutViewModel>

    private val viewModel: RiskyContactIsolationOptOutViewModel by viewModels { factory }

    private lateinit var binding: ActivityRiskyContactIsolationOptOutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityRiskyContactIsolationOptOutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configureToolbar()
        setAccessibilityText()
        setClickListeners()
    }

    private fun setAccessibilityText() {
        binding.nhsGuidanceLinkTextView.setUpOpensInBrowserWarning()
        binding.primaryActionButton.setUpOpensInBrowserWarning()
    }

    private fun setClickListeners() {
        binding.secondaryActionButton.setOnSingleClickListener {
            viewModel.acknowledgeAndOptOutContactIsolation()
            StatusActivity.start(this)
        }

        binding.primaryActionButton.setOnSingleClickListener {
            viewModel.acknowledgeAndOptOutContactIsolation()
            openInExternalBrowserForResult(getString(R.string.risky_contact_opt_out_primary_button_url), REQUEST_ADVICE_EXTERNAL_LINK)
        }

        binding.nhsGuidanceLinkTextView.setOnSingleClickListener {
            viewModel.acknowledgeAndOptOutContactIsolation()
            openInExternalBrowserForResult(getString(R.string.risky_contact_opt_out_further_advice_link_url), REQUEST_ADVICE_EXTERNAL_LINK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADVICE_EXTERNAL_LINK) {
            StatusActivity.start(this)
        }
    }

    private fun configureToolbar() =
        setNavigateUpToolbar(
            binding.primaryToolbar.toolbar,
            titleResId = R.string.empty,
            upIndicator = R.drawable.ic_arrow_back_primary
        )

    companion object {
        private const val REQUEST_ADVICE_EXTERNAL_LINK = 1124
    }
}
