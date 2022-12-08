package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityTestOriginBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestOriginViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestOriginViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class TestOriginActivity : BaseActivity() {

    @Inject
    lateinit var factory: TestOriginViewModel.Factory

    private val viewModel: TestOriginViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivityTestOriginBinding

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityTestOriginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_test_origin_back_button_accessibility_description
        )

        binding.testOriginBinaryVerticalRadioGroup.setOnValueChangedListener(viewModel::onTestOriginOptionChecked)

        setClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            binding.testOriginBinaryVerticalRadioGroup.selectedOption = viewState.testOriginSelection
            showError(viewState.hasError)
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is TestDate -> {
                    startActivity<SelectTestDateActivity> {
                        putExtra(
                            SelectTestDateActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is TestKitType -> {
                    startActivity<TestKitTypeActivity> {
                        putExtra(
                            TestKitTypeActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
            }
        }
    }

    private fun setClickListeners() {
        binding.selfReportTestOriginContinueButton.setOnSingleClickListener {
            viewModel.onClickContinue()
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun showError(hasError: Boolean) = with(binding) {
        if (hasError) {
            selfReportTestOriginErrorView.visible()
            selfReportTestOriginErrorIndicator.visible()
            selfReportTestOriginErrorText.visible()
            if (!hasScrolledToError) {
                selfReportTestOriginScrollViewContainer.smoothScrollToAndThen(0, 0) {
                    selfReportTestOriginErrorView.requestFocusFromTouch()
                    selfReportTestOriginErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            selfReportTestOriginErrorView.gone()
            selfReportTestOriginErrorIndicator.invisible()
            selfReportTestOriginErrorText.gone()
        }
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
    }
}
