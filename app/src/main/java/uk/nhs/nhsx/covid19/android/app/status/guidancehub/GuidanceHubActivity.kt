package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityGuidanceHubBinding
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.util.viewutils.openInExternalBrowserForResult
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class GuidanceHubActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<GuidanceHubViewModel>
    private val viewModel: GuidanceHubViewModel by viewModels { factory }

    private lateinit var binding: ActivityGuidanceHubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityGuidanceHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbar(
            binding.titleToolbar.root,
            R.string.home_covid19_guidance_button_title,
            upIndicator = R.drawable.ic_arrow_back_white
        )

        setupOnClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.navigationTarget().observe(this) { navTarget ->
            val url = when (navTarget) {
                is NavigationTarget.EnglandGuidance -> getString(R.string.covid_guidance_hub_for_england_url)
                is NavigationTarget.CheckSymptomsGuidance -> getString(R.string.covid_guidance_hub_check_symptoms_url)
                is NavigationTarget.LatestGuidance -> getString(R.string.covid_guidance_hub_latest_url)
                is NavigationTarget.PositiveTestResultGuidance -> getString(R.string.covid_guidance_hub_positive_test_result_url)
                is NavigationTarget.TravellingAbroadGuidance -> getString(R.string.covid_guidance_hub_travelling_abroad_url)
                is NavigationTarget.CheckSSPGuidance -> getString(R.string.covid_guidance_hub_check_ssp_url)
                is NavigationTarget.CovidEnquiryGuidance -> getString(R.string.covid_guidance_hub_enquiries_url)
            }
            openInExternalBrowserForResult(url)
        }
    }

    private fun setupOnClickListeners() {
        with(binding) {
            itemForEnglandGuidance.setOnSingleClickListener {
                viewModel.itemForEnglandGuidanceClicked()
            }

            itemCheckSymptomsGuidance.setOnSingleClickListener {
                viewModel.itemCheckSymptomsGuidanceClicked()
            }

            itemLatestGuidance.setOnSingleClickListener {
                viewModel.itemLatestGuidanceClicked()
            }

            itemPositiveTestResultGuidance.setOnSingleClickListener {
                viewModel.itemPositiveTestResultGuidanceClicked()
            }

            itemTravellingAbroadGuidance.setOnSingleClickListener {
                viewModel.itemTravellingAbroadGuidanceClicked()
            }

            itemCheckSSPGuidance.setOnSingleClickListener {
                viewModel.itemCheckSSPGuidanceClicked()
            }

            itemCovidEnquiriesGuidance.setOnSingleClickListener {
                viewModel.itemCovidEnquiriesGuidanceClicked()
            }
        }
    }
}
