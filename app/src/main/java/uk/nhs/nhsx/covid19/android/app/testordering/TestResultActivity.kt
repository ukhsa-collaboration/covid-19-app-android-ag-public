package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_test_result.goodNewsContainer
import kotlinx.android.synthetic.main.activity_test_result.isolationRequestContainer
import kotlinx.android.synthetic.main.view_good_news.goodNewsActionButton
import kotlinx.android.synthetic.main.view_good_news.goodNewsIcon
import kotlinx.android.synthetic.main.view_good_news.goodNewsInfoView
import kotlinx.android.synthetic.main.view_good_news.goodNewsParagraphContainer
import kotlinx.android.synthetic.main.view_good_news.goodNewsSubtitle
import kotlinx.android.synthetic.main.view_good_news.goodNewsTitle
import kotlinx.android.synthetic.main.view_isolation_request.accessibilityContainer
import kotlinx.android.synthetic.main.view_isolation_request.exposureFaqsLink
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestActionButton
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestImage
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestInfoView
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestParagraphContainer
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestTitle1
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestTitle2
import kotlinx.android.synthetic.main.view_isolation_request.isolationRequestTitle3
import kotlinx.android.synthetic.main.view_toolbar_background.toolbar
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.exposure.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.inPortraitMode
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveThenNegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.MainState.VoidWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.NavigationEvent.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.NavigationEvent.NavigateToOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewModel.NavigationEvent.NavigateToShareKeys
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class TestResultActivity : BaseActivity(R.layout.activity_test_result) {

    @Inject
    lateinit var factory: ViewModelFactory<TestResultViewModel>

    private val viewModel: TestResultViewModel by viewModels { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        startViewModelListeners()

        viewModel.onCreate()
    }

    private fun startViewModelListeners() {
        viewModel.navigationEvent().observe(this) { navigationEvent ->
            when (navigationEvent) {
                is NavigateToShareKeys -> ShareKeysInformationActivity.start(
                    this,
                    navigationEvent.testResult
                )
                NavigateToOrderTest -> startActivityForResult(
                    TestOrderingActivity.getIntent(this),
                    REQUEST_CODE_ORDER_A_TEST
                )
                Finish -> finish()
            }
        }

        viewModel.viewState().observe(
            this,
            Observer { viewState ->
                when (viewState.mainState) {
                    NegativeNotInIsolation ->
                        showAreNotIsolatingScreenOnNegative()
                    NegativeWillBeInIsolation ->
                        showContinueToSelfIsolationScreenOnNegative(viewState.remainingDaysInIsolation)
                    NegativeWontBeInIsolation ->
                        showDoNotHaveToSelfIsolateScreenOnNegative()
                    PositiveContinueIsolation ->
                        showContinueToSelfIsolationScreenOnPositive(viewState.remainingDaysInIsolation)
                    PositiveContinueIsolationNoChange ->
                        showContinueToSelfIsolationScreenOnPositiveAndNoChange(viewState.remainingDaysInIsolation)
                    PositiveWillBeInIsolation ->
                        showSelfIsolateScreenOnPositive(viewState.remainingDaysInIsolation)
                    PositiveWontBeInIsolation ->
                        showDoNotHaveToSelfIsolateScreenOnPositive()
                    PositiveThenNegativeWillBeInIsolation ->
                        showContinueToSelfIsolationScreenOnPositiveThenNegative(viewState.remainingDaysInIsolation)
                    PositiveWillBeInIsolationAndOrderTest ->
                        showSelfIsolateScreenOnPositiveAndOrderTest(viewState.remainingDaysInIsolation)
                    VoidNotInIsolation ->
                        showAreNotIsolatingScreenOnVoid()
                    VoidWillBeInIsolation ->
                        showContinueToSelfIsolationScreenOnVoid(viewState.remainingDaysInIsolation)
                    Ignore -> finish()
                }
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_ORDER_A_TEST) {
            navigateToStatusActivity()
        } else {
            finish()
        }
    }

    private fun navigateToStatusActivity() {
        StatusActivity.start(this)
        finish()
    }

    private fun showContinueToSelfIsolationScreenOnPositive(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.visible()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_positive_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_positive_continue_self_isolate_explanation_1),
            getString(R.string.test_result_positive_continue_self_isolate_explanation_2),
            getString(R.string.exposure_faqs_title)
        )

        isolationRequestActionButton.text = getString(R.string.continue_button)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showContinueToSelfIsolationScreenOnPositiveAndNoChange(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.visible()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_positive_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_positive_continue_self_isolate_no_change_explanation_1),
            getString(R.string.exposure_faqs_title)
        )

        isolationRequestActionButton.text = getString(R.string.continue_button)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showContinueToSelfIsolationScreenOnNegative(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.gone()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )

        isolationRequestInfoView.stateText = getString(R.string.state_test_negative_info)
        isolationRequestInfoView.stateColor = getColor(R.color.amber)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_negative_continue_self_isolate_explanation)
        )

        isolationRequestActionButton.text = getString(R.string.back_to_home)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showContinueToSelfIsolationScreenOnVoid(remainingDaysInIsolation: Int) {
        setCloseToolbar()

        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.gone()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_book_test)

        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_void_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_void_continue_self_isolate_explanation)
        )

        isolationRequestActionButton.text = getString(R.string.book_free_test)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.onBackPressed()
    }

    private fun showDoNotHaveToSelfIsolateScreenOnPositive() {
        goodNewsContainer.visible()
        isolationRequestContainer.gone()

        goodNewsIcon.setImageResource(R.drawable.ic_isolation_expired_or_over)
        goodNewsIcon.isVisible = inPortraitMode()
        goodNewsTitle.text = getString(R.string.test_result_your_test_result)
        title = goodNewsTitle.text
        goodNewsTitle.visible()

        goodNewsSubtitle.text = getString(R.string.test_result_positive_no_self_isolation_subtitle)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(getString(R.string.for_further_advice_visit))

        goodNewsActionButton.text = getString(R.string.continue_button)
        goodNewsActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showDoNotHaveToSelfIsolateScreenOnNegative() {
        goodNewsContainer.visible()
        isolationRequestContainer.gone()

        goodNewsIcon.setImageResource(R.drawable.ic_isolation_negative_or_finished)
        goodNewsIcon.isVisible = inPortraitMode()
        goodNewsTitle.visible()
        title = goodNewsTitle.text

        goodNewsSubtitle.text =
            getString(R.string.test_result_negative_no_self_isolation_subtitle_text)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(
            getString(R.string.for_further_advice_visit)
        )

        goodNewsActionButton.text = getString(R.string.continue_button)
        goodNewsActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showAreNotIsolatingScreenOnNegative() {
        goodNewsContainer.visible()

        isolationRequestContainer.gone()

        goodNewsIcon.setImageResource(R.drawable.ic_isolation_expired_or_over)
        goodNewsIcon.isVisible = inPortraitMode()
        goodNewsTitle.visible()
        title = goodNewsTitle.text

        goodNewsSubtitle.text =
            getString(R.string.test_result_negative_already_not_in_isolation_subtitle)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(getString(R.string.for_further_advice_visit))

        goodNewsActionButton.text = getString(R.string.continue_button)
        goodNewsActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun setSelfIsolateTitles(
        title1: String,
        title2: String,
        title3: String? = null
    ) {
        isolationRequestTitle1.text = title1
        isolationRequestTitle2.text = title2
        isolationRequestTitle3.text = title3
        isolationRequestTitle3.isVisible = title3 != null

        with("$title1 $title2 ${title3.orEmpty()}") {
            title = this
            accessibilityContainer.contentDescription = this
            accessibilityContainer.setUpAccessibilityHeading()
        }
        listOf(
            isolationRequestTitle1,
            isolationRequestTitle2,
            isolationRequestTitle3
        ).forEach {
            it.contentDescription = null
        }
    }

    private fun showContinueToSelfIsolationScreenOnPositiveThenNegative(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()

        exposureFaqsLink.gone()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.test_result_positive_continue_self_isolation_title_1),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )

        isolationRequestInfoView.stateText =
            getString(R.string.state_test_positive_then_negative_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_positive_then_negative_explanation)
        )

        isolationRequestActionButton.text = getString(R.string.continue_button)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showSelfIsolateScreenOnPositive(remainingDaysInIsolation: Int) {
        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.visible()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_continue)
        setSelfIsolateTitles(
            getString(R.string.self_isolate_for),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            )
        )
        isolationRequestInfoView.stateText = getString(R.string.state_test_positive_info)
        isolationRequestInfoView.stateColor = getColor(R.color.error_red)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_negative_then_positive_continue_explanation),
            getString(R.string.exposure_faqs_title)
        )

        isolationRequestActionButton.text = getString(R.string.continue_button)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showSelfIsolateScreenOnPositiveAndOrderTest(remainingDaysInIsolation: Int) {
        setCloseToolbar()

        goodNewsContainer.gone()
        isolationRequestContainer.visible()
        exposureFaqsLink.visible()

        isolationRequestImage.setImageResource(R.drawable.ic_isolation_book_test)

        setSelfIsolateTitles(
            getString(R.string.self_isolate_for),
            resources.getQuantityString(
                R.plurals.state_isolation_days,
                remainingDaysInIsolation,
                remainingDaysInIsolation
            ),
            getString(R.string.test_result_positive_self_isolate_and_book_test_title_3)
        )
        isolationRequestInfoView.stateText =
            getString(R.string.state_test_positive_and_book_test_info)
        isolationRequestInfoView.stateColor = getColor(R.color.amber)
        isolationRequestParagraphContainer.addAllParagraphs(
            getString(R.string.test_result_positive_self_isolate_and_book_test_explanation_1),
            getString(R.string.exposure_faqs_title)
        )

        isolationRequestActionButton.text = getString(R.string.book_follow_up_test)
        isolationRequestActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showAreNotIsolatingScreenOnVoid() {
        setCloseToolbar()

        goodNewsContainer.visible()
        isolationRequestContainer.gone()

        goodNewsIcon.setImageResource(R.drawable.ic_isolation_expired_or_over)
        goodNewsIcon.isVisible = inPortraitMode()
        goodNewsTitle.text = getString(R.string.test_result_your_test_result)
        title = getString(R.string.test_result_your_test_result)
        goodNewsTitle.visible()

        goodNewsSubtitle.text =
            getString(R.string.test_result_void_already_not_in_isolation_subtitle)
        goodNewsInfoView.stateText =
            getString(R.string.test_result_no_self_isolation_description)
        goodNewsInfoView.stateColor = getColor(R.color.amber)
        goodNewsParagraphContainer.addAllParagraphs(getString(R.string.for_further_advice_visit))

        goodNewsActionButton.text = getString(R.string.book_free_test)
        goodNewsActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun setCloseToolbar() {
        setCloseToolbar(
            toolbar,
            R.string.empty,
            R.drawable.ic_close_primary
        )
    }

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1339
    }
}
