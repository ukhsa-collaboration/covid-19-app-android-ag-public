package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelfReportNegativeVoidTestResultBinding
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportNegativeVoidTestResultViewModel.NavigationTarget.Status
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import javax.inject.Inject

class SelfReportNegativeVoidTestResultActivity : BaseActivity() {

    @Inject
    lateinit var factory: SelfReportNegativeVoidTestResultViewModel.Factory
    private val viewModel: SelfReportNegativeVoidTestResultViewModel by assistedViewModel {
        factory.create(
            isNegative = intent.getBooleanExtra(TEST_RESULT, false)
        )
    }

    private lateinit var binding: ActivitySelfReportNegativeVoidTestResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelfReportNegativeVoidTestResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_negative_or_void_test_result_back_button_accessibility_description
        )

        setupObservers()

        viewModel.fetchCountry()

        setClickListeners()
    }

    private fun setClickListeners() {
        binding.buttonReturnToHomeScreen.setOnSingleClickListener {
            viewModel.onClickBackToHome()
        }
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            if (viewState.country == WALES) { setupWalesUI() }
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is Status -> {
                    StatusActivity.start(this)
                    finish()
                }
            }
        }
    }

    private fun setupWalesUI() {
        binding.englandSelfReportedEligibleText.gone()
        binding.englandSelfReportedLink.gone()
    }

    companion object {
        const val TEST_RESULT = "TEST_RESULT"
    }
}
