package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityRiskyContactIsolationOptOutBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpOpensInBrowserWarning
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
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
        setUpView()
        configureToolbar()
        setAccessibilityText()
    }

    private fun setUpView() {
        viewModel.localAuthorityPostCode.observe(this) { supportedCountry ->
            when (supportedCountry) {
                ENGLAND -> setUpViewForEngland()
                else -> setUpViewForWales()
            }
        }
        viewModel.updateViewState()
    }

    private fun setUpViewForWales() {
        with(binding) {
            riskyContactAdviceTitle.text = getString(R.string.risky_contact_opt_out_advice_title_wales)
            riskyContactAdviceFreshAir.updateText(getString(R.string.risky_contact_opt_out_advice_meeting_indoors_wales))
            riskyContactAdviceFaceCovering.updateText(getString(R.string.risky_contact_opt_out_advice_mask_wales))
            riskyContactAdviceTestingHub.updateText(getString(R.string.risky_contact_opt_out_advice_testing_hub_wales))
            riskyContactAdviceWashHands.updateText(getString(R.string.risky_contact_opt_out_advice_wash_hands_wales))
            primaryActionButton.text = getString(R.string.risky_contact_opt_out_primary_button_title_wales)
            secondaryActionButton.text = getString(R.string.risky_contact_opt_out_secondary_button_title_wales)
            riskyContactAdviceTestingHub.visible()
        }

        setClickListeners(getString(R.string.risky_contact_opt_out_primary_button_url_wales))
    }

    private fun setUpViewForEngland() {
        with(binding) {
            riskyContactAdviceTitle.text = getString(R.string.risky_contact_opt_out_advice_title)
            riskyContactAdviceFreshAir.updateText(getString(R.string.risky_contact_opt_out_advice_meeting_indoors))
            riskyContactAdviceFaceCovering.updateText(getString(R.string.risky_contact_opt_out_advice_mask))
            riskyContactAdviceWashHands.updateText(getString(R.string.risky_contact_opt_out_advice_wash_hands))
            primaryActionButton.text = getString(R.string.risky_contact_opt_out_primary_button_title)
            secondaryActionButton.text = getString(R.string.risky_contact_opt_out_secondary_button_title)
            riskyContactAdviceTestingHub.gone()
        }

        setClickListeners(getString(R.string.risky_contact_opt_out_primary_button_url))
    }

    private fun setAccessibilityText() {
        binding.primaryActionButton.setUpOpensInBrowserWarning()
    }

    private fun setClickListeners(primaryButtonUrl: String) {
        binding.secondaryActionButton.setOnSingleClickListener {
            viewModel.acknowledgeAndOptOutContactIsolation()
            StatusActivity.start(this)
        }

        binding.primaryActionButton.setOnSingleClickListener {
            viewModel.acknowledgeAndOptOutContactIsolation()
            openInExternalBrowserForResult(primaryButtonUrl, REQUEST_ADVICE_EXTERNAL_LINK)
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
