package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivitySelfReportSymptomsBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.SymptomOnsetDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportSymptomsViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class SelfReportSymptomsActivity : BaseActivity() {

    @Inject
    lateinit var factory: SelfReportSymptomsViewModel.Factory

    private val viewModel: SelfReportSymptomsViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivitySelfReportSymptomsBinding

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivitySelfReportSymptomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_symptoms_back_button_accessibility_description
        )

        binding.selfReportSymptomsBinaryRadioGroup.setOnValueChangedListener(viewModel::onSymptomsOptionChecked)

        setClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            binding.selfReportSymptomsBinaryRadioGroup.selectedOption = viewState.symptomsSelection
            showError(viewState.hasError)
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is SymptomOnsetDate -> {
                    startActivity<SelfReportSymptomsOnsetActivity> {
                        putExtra(
                            SelfReportSymptomsOnsetActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
                is ReportedTest -> {
                    startActivity<ReportedTestActivity> {
                        putExtra(
                            ReportedTestActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
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
                is CheckAnswers -> {
                    startActivity<SelfReportCheckAnswersActivity> {
                        putExtra(
                            SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY,
                            navTarget.selfReportTestQuestions
                        )
                    }
                    finish()
                }
            }
        }
    }

    private fun setClickListeners() {
        binding.selfReportSymptomsContinueButton.setOnSingleClickListener {
            viewModel.onClickContinue()
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun showError(hasError: Boolean) = with(binding) {
        if (hasError) {
            selfReportSymptomsErrorView.visible()
            selfReportSymptomsErrorIndicator.visible()
            selfReportSymptomsErrorText.visible()
            if (!hasScrolledToError) {
                selfReportSymptomsScrollViewContainer.smoothScrollToAndThen(0, 0) {
                    selfReportSymptomsErrorView.requestFocusFromTouch()
                    selfReportSymptomsErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            selfReportSymptomsErrorView.gone()
            selfReportSymptomsErrorIndicator.invisible()
            selfReportSymptomsErrorText.gone()
        }
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
    }
}
