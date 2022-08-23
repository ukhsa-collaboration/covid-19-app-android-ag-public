package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityGuidanceHubBinding
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubViewModel.NavigationTarget.ExternalLink
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubViewModel.NewLabelViewState.Hidden
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubViewModel.NewLabelViewState.Visible
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

        viewModel.onCreate()
    }

    private fun setupObservers() {
        viewModel.navigationTarget().observe(this) { navTarget ->
            val url = when (navTarget) {
                is ExternalLink -> {
                    getString(navTarget.urlRes)
                }
            }
            openInExternalBrowserForResult(url)
        }

        viewModel.newLabelViewState().observe(this) { state ->
            when (state) {
                is Hidden -> {
                    binding.itemSeven.newLabelAccessibilityText = null
                    binding.itemSeven.shouldDisplayNewLabel = false
                }
                is Visible -> {
                    binding.itemSeven.newLabelAccessibilityText =
                        getString(R.string.covid_guidance_hub_england_button_seven_new_label_accessibility_text)
                    binding.itemSeven.shouldDisplayNewLabel = true
                }
            }
        }
    }

    private fun setupOnClickListeners() {
        with(binding) {
            itemOne.setOnSingleClickListener {
                viewModel.itemOneClicked()
            }

            itemTwo.setOnSingleClickListener {
                viewModel.itemTwoClicked()
            }

            itemThree.setOnSingleClickListener {
                viewModel.itemThreeClicked()
            }

            itemFour.setOnSingleClickListener {
                viewModel.itemFourClicked()
            }

            itemFive.setOnSingleClickListener {
                viewModel.itemFiveClicked()
            }

            itemSix.setOnSingleClickListener {
                viewModel.itemSixClicked()
            }

            itemSeven.setOnSingleClickListener {
                viewModel.itemSevenClicked()
            }

            itemEight.setOnSingleClickListener {
                viewModel.itemEightClicked()
            }
        }
    }
}
