package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.assistedViewModel
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityTestKitTypeBinding
import uk.nhs.nhsx.covid19.android.app.startActivity
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.DeclinedKeySharing
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.ShareKeysInfo
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestKitTypeViewModel.NavigationTarget.TestOrigin
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.invisible
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.smoothScrollToAndThen
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbarWithoutTitle
import javax.inject.Inject

class TestKitTypeActivity : BaseActivity() {

    @Inject
    lateinit var factory: TestKitTypeViewModel.Factory

    private val viewModel: TestKitTypeViewModel by assistedViewModel {
        factory.create(
            questions = intent.getParcelableExtra(SELF_REPORT_QUESTIONS_DATA_KEY)
                ?: throw IllegalStateException("self report questions data was not available from starting intent")
        )
    }

    private lateinit var binding: ActivityTestKitTypeBinding

    private var hasScrolledToError: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityTestKitTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNavigateUpToolbarWithoutTitle(
            binding.primaryToolbar.toolbar,
            upIndicator = R.drawable.ic_arrow_back_white,
            upContentDescription = R.string.self_report_test_kit_type_back_button_accessibility_description
        )

        binding.testKitTypeBinaryVerticalRadioGroup.setOnValueChangedListener(viewModel::onTestKitTypeOptionChecked)

        setClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.viewState().observe(this) { viewState ->
            binding.testKitTypeBinaryVerticalRadioGroup.selectedOption = viewState.testKitTypeSelection
            showError(viewState.hasError)
        }

        viewModel.navigate().observe(this) { navTarget ->
            when (navTarget) {
                is TestOrigin -> {
                    startActivity<TestOriginActivity> {
                        putExtra(TestOriginActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
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
                is DeclinedKeySharing -> {
                    startActivity<SelfReportAppWillNotNotifyOtherUsersActivity> {
                        putExtra(SelfReportAppWillNotNotifyOtherUsersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
                    }
                    finish()
                }
                is ShareKeysInfo -> {
                    startActivity<SelfReportShareKeysInformationActivity> {
                        putExtra(SelfReportShareKeysInformationActivity.SELF_REPORT_QUESTIONS_DATA_KEY, navTarget.selfReportTestQuestions)
                    }
                    finish()
                }
            }
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun setClickListeners() {
        binding.selfReportTestKitTypeContinueButton.setOnSingleClickListener {
            viewModel.onClickContinue()
        }
    }

    private fun showError(hasError: Boolean) = with(binding) {
        if (hasError) {
            selfReportTestKitTypeErrorView.visible()
            selfReportTestKitTypeErrorIndicator.visible()
            selfReportTestKitTypeErrorText.visible()
            if (!hasScrolledToError) {
                selfReportTestKitTypeScrollViewContainer.smoothScrollToAndThen(0, 0) {
                    selfReportTestKitTypeErrorView.requestFocusFromTouch()
                    selfReportTestKitTypeErrorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    hasScrolledToError = true
                }
            }
        } else {
            selfReportTestKitTypeErrorView.gone()
            selfReportTestKitTypeErrorIndicator.invisible()
            selfReportTestKitTypeErrorText.gone()
        }
    }

    companion object {
        const val SELF_REPORT_QUESTIONS_DATA_KEY = "SELF_REPORT_QUESTIONS_DATA_KEY"
    }
}
