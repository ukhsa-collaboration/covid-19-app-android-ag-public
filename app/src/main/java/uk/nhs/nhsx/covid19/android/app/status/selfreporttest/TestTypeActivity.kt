package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityTestTypeBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestTypeViewModel.NavigationTarget.NegativeTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestTypeViewModel.NavigationTarget.PositiveTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestTypeViewModel.NavigationTarget.VoidTest
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithCustomBackButtonDescriptionResId
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class TestTypeActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<TestTypeViewModel>
    private val viewModel: TestTypeViewModel by viewModels { factory }

    private lateinit var binding: ActivityTestTypeBinding

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityTestTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            setNavigateUpToolbarWithCustomBackButtonDescriptionResId(
                primaryToolbar.toolbar,
                R.string.self_report_test_type_title,
                R.string.self_report_test_type_back_button_accessibility_description,
                upIndicator = R.drawable.ic_arrow_back_white
            )
            testTypeTripleVerticalRadioGroup.setOnValueChangedListener(viewModel::onTestTypeOptionChecked)

            selfReportTestTypeContinueButton.setOnSingleClickListener {
                viewModel.onClickContinue()
            }
        }

        setupObservers()
        val selfReportTestQuestions = intent.getParcelableExtra<SelfReportTestQuestions>(SELF_REPORT_QUESTIONS_DATA_KEY)
        viewModel.onCreate(selfReportTestQuestions)
    }

    private fun showError(hasError: Boolean) = with(binding) {
        if (hasError) {
            selfReportTestTypeErrorView.visible()
            enterTestTypeErrorIndicator.visible()
            selfReportTestTypeErrorText.visible()
            if (!hasScrolledToError) {
                selfReportTestTypeScrollViewContainer.smoothScrollToAndThen(0, 0) {
                    selfReportTestTypeErrorView.requestFocusFromTouch()
                    selfReportTestTypeErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            selfReportTestTypeErrorView.gone()
            enterTestTypeErrorIndicator.invisible()
            selfReportTestTypeErrorText.gone()
        }
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            binding.testTypeTripleVerticalRadioGroup.selectedOption = viewState.testTypeSelection
            showError(viewState.hasError)
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is PositiveTest -> {
                    startActivity<SelfReportShareKeysInformationActivity> {
                        putExtra(SelfReportShareKeysInformationActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
                    }
                    finish()
                }
                is NegativeTest -> {
                    startActivity<SelfReportNegativeVoidTestResultActivity> {
                        putExtra(SelfReportNegativeVoidTestResultActivity.TEST_RESULT, navTarget.isNegative)
                    }
                }
                is VoidTest -> {
                    startActivity<SelfReportNegativeVoidTestResultActivity> {
                        putExtra(SelfReportNegativeVoidTestResultActivity.TEST_RESULT, navTarget.isNegative)
                    }
                }
            }
        }
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
    }
}
