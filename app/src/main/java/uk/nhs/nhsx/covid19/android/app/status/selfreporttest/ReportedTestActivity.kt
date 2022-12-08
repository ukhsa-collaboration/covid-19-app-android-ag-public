package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityReportedTestBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.SymptomsOnset
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class ReportedTestActivity : BaseActivity() {

    @Inject
    lateinit var factory: ReportedTestViewModel.Factory

    private val viewModel: ReportedTestViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivityReportedTestBinding

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityReportedTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_reported_test_back_button_accessibility_description
        )

        binding.selfReportReportedTestBinaryVerticalRadioGroup.setOnValueChangedListener(viewModel::onReportedTestOptionChecked)

        setClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            binding.selfReportReportedTestBinaryVerticalRadioGroup.selectedOption = viewState.reportedTestSelection
            showError(viewState.hasError)
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is CheckAnswers -> {
                    startActivity<SelfReportCheckAnswersActivity> {
                        putExtra(
                            SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is SymptomsOnset -> {
                    startActivity<SelfReportSymptomsOnsetActivity> {
                        putExtra(
                            SelfReportSymptomsOnsetActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is Symptoms -> {
                    startActivity<SelfReportSymptomsActivity> {
                        putExtra(
                            SelfReportSymptomsActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is TestDate -> {
                    startActivity<SelectTestDateActivity> {
                        putExtra(
                            SelectTestDateActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
            }
        }
    }

    private fun setClickListeners() {
        binding.selfReportReportedTestContinueButton.setOnSingleClickListener {
            viewModel.onClickContinue()
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun showError(hasError: Boolean) = with(binding) {
        if (hasError) {
            selfReportReportedTestErrorView.visible()
            selfReportReportedTestErrorIndicator.visible()
            selfReportReportedTestErrorText.visible()
            if (!hasScrolledToError) {
                selfReportReportedTestScrollViewContainer.smoothScrollToAndThen(0, 0) {
                    selfReportReportedTestErrorView.requestFocusFromTouch()
                    selfReportReportedTestErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            selfReportReportedTestErrorView.gone()
            selfReportReportedTestErrorIndicator.invisible()
            selfReportReportedTestErrorText.gone()
        }
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
    }
}
