package uk.nhs.nhsx.covid19.android.app.status.guidancehub

import android.os.Bundle
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityGuidanceHubBinding
import uk.nhs.nhsx.covid19.android.app.status.guidancehub.GuidanceHubViewModel.NavigationTarget.ExternalLink
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
                is ExternalLink -> {
                    getString(navTarget.urlRes)
                }
            }
            openInExternalBrowserForResult(url)
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
