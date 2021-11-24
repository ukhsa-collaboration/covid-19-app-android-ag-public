package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.ViewModelFactory
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityTestResultBinding
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.NavigateToOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.NavigateToShareKeys
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PlodWillContinueWithCurrentState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setCloseToolbar
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import javax.inject.Inject

class TestResultActivity : BaseActivity() {

    @Inject
    lateinit var factory: ViewModelFactory<BaseTestResultViewModel>

    private val viewModel: BaseTestResultViewModel by viewModels { factory }

    private lateinit var binding: ActivityTestResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        binding = ActivityTestResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startViewModelListeners()
    }

    private fun startViewModelListeners() {
        viewModel.navigationEvent().observe(this) { navigationEvent ->
            when (navigationEvent) {
                is NavigateToShareKeys -> startActivityForResult(
                    ShareKeysInformationActivity.getIntent(this, navigationEvent.bookFollowUpTest),
                    REQUEST_CODE_SHARE_KEYS
                )
                NavigateToOrderTest -> startActivityForResult(
                    TestOrderingActivity.getIntent(this),
                    REQUEST_CODE_ORDER_A_TEST
                )
                Finish -> finish()
            }
        }

        viewModel.viewState().observe(this) { viewState ->
            when (viewState.mainState) {
                NegativeNotInIsolation ->
                    showAreNotIsolatingScreenOnNegative()
                NegativeWillBeInIsolation ->
                    showContinueToSelfIsolationScreenOnNegative(viewState.remainingDaysInIsolation)
                NegativeWontBeInIsolation ->
                    showDoNotHaveToSelfIsolateScreenOnNegative()
                is PositiveContinueIsolation ->
                    showContinueToSelfIsolationScreenOnPositive(viewState.remainingDaysInIsolation)
                PositiveContinueIsolationNoChange ->
                    showContinueToSelfIsolationScreenOnPositiveAndNoChange(viewState.remainingDaysInIsolation)
                is PositiveWillBeInIsolation ->
                    showSelfIsolateScreenOnPositive(viewState.remainingDaysInIsolation)
                is PositiveWontBeInIsolation ->
                    showDoNotHaveToSelfIsolateScreenOnPositive()
                NegativeAfterPositiveOrSymptomaticWillBeInIsolation ->
                    showContinueToSelfIsolationScreenOnNegativeAfterPositiveOrSymptomatic(viewState.remainingDaysInIsolation)
                PositiveWillBeInIsolationAndOrderTest ->
                    showSelfIsolateScreenOnPositiveAndOrderTest(viewState.remainingDaysInIsolation)
                VoidNotInIsolation ->
                    showAreNotIsolatingScreenOnVoid()
                VoidWillBeInIsolation ->
                    showContinueToSelfIsolationScreenOnVoid(viewState.remainingDaysInIsolation)
                PlodWillContinueWithCurrentState ->
                    showContinueWithCurrentStateScreenOnPlod()
                Ignore -> finish()
            }
        }
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

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.onBackPressed()
    }

    private fun setCloseToolbar() {
        setCloseToolbar(
            binding.primaryToolbar.toolbar,
            R.string.empty,
            R.drawable.ic_close_primary
        )
    }

    //region Isolation states
    private fun setSelfIsolateTitles(
        title1: String,
        title2: String,
        title3: String? = null
    ) = with(binding.isolationRequestContainer) {

        isolationRequestTitle1.text = title1
        isolationRequestTitle2.text = title2
        isolationRequestTitle3.text = title3
        isolationRequestTitle3.isVisible = title3 != null

        listOf(
            isolationRequestTitle1,
            isolationRequestTitle2,
            isolationRequestTitle3
        ).forEach {
            it.contentDescription = null
        }

        with("$title1 $title2 ${title3.orEmpty()}") {
            title = this
            accessibilityContainer.contentDescription = this
            accessibilityContainer.setUpAccessibilityHeading()
        }
    }

    private fun showIsolationState(
        hasCloseToolbar: Boolean = false,
        remainingDaysInIsolation: Int,
        @DrawableRes iconResource: Int = R.drawable.ic_isolation_continue,
        @StringRes stateText: Int = R.string.state_test_positive_info,
        @ColorRes stateColor: Int = R.color.error_red,
        @StringRes selfIsolationLabel: Int = R.string.test_result_positive_continue_self_isolation_title_1,
        @StringRes additionalIsolationInfoText: Int? = null,
        @StringRes actionButtonStringResource: Int = R.string.continue_button,
        exposureLinksVisible: Boolean,
        @StringRes onlineServiceLinkText: Int = R.string.nhs_111_online_service,
        @StringRes onlineServiceLinkUrl: Int = R.string.url_nhs_111_online,
        @StringRes vararg paragraphResources: Int
    ) = with(binding) {
        if (hasCloseToolbar) {
            setCloseToolbar()
        }
        goodNewsContainer.root.gone()
        with(isolationRequestContainer) {

            root.visible()
            exposureFaqsLink.isVisible = exposureLinksVisible

            isolationRequestImage.setImageResource(iconResource)

            setSelfIsolateTitles(
                getString(selfIsolationLabel),
                resources.getQuantityString(
                    R.plurals.state_isolation_days,
                    remainingDaysInIsolation,
                    remainingDaysInIsolation
                ),
                if (additionalIsolationInfoText == null) null else getString(additionalIsolationInfoText)
            )

            isolationRequestContainer.isolationRequestInfoView.apply {
                this.stateText = getString(stateText)
                this.stateColor = getColor(stateColor)
            }

            isolationRequestParagraphContainer.addAllParagraphs(paragraphResources.map { getString(it) })

            isolationRequestOnlineServiceLink.setDisplayText(onlineServiceLinkText)
            isolationRequestOnlineServiceLink.setLinkUrl(onlineServiceLinkUrl)

            isolationRequestActionButton.apply {
                text = getString(actionButtonStringResource)
                setOnSingleClickListener { viewModel.onActionButtonClicked() }
            }
        }
    }

    private fun showContinueToSelfIsolationScreenOnPositive(remainingDaysInIsolation: Int) {
        showIsolationState(
            remainingDaysInIsolation = remainingDaysInIsolation,
            exposureLinksVisible = true,
            paragraphResources = intArrayOf(
                R.string.test_result_positive_continue_self_isolate_explanation_1,
                R.string.test_result_positive_continue_self_isolate_explanation_2,
                R.string.exposure_faqs_title
            )
        )
    }

    private fun showContinueToSelfIsolationScreenOnPositiveAndNoChange(remainingDaysInIsolation: Int) {
        showIsolationState(
            remainingDaysInIsolation = remainingDaysInIsolation,
            exposureLinksVisible = true,
            paragraphResources = intArrayOf(
                R.string.test_result_positive_continue_self_isolate_no_change_explanation_1,
                R.string.exposure_faqs_title
            )
        )
    }

    private fun showContinueToSelfIsolationScreenOnNegative(remainingDaysInIsolation: Int) {
        showIsolationState(
            remainingDaysInIsolation = remainingDaysInIsolation,
            exposureLinksVisible = false,
            stateText = R.string.state_test_negative_info,
            stateColor = R.color.amber,
            actionButtonStringResource = R.string.back_to_home,
            onlineServiceLinkText = R.string.test_result_negative_continue_self_isolate_nhs_guidance_label,
            onlineServiceLinkUrl = R.string.url_nhs_guidance,
            paragraphResources = intArrayOf(
                R.string.test_result_negative_continue_self_isolate_explanation
            )
        )
    }

    private fun showContinueToSelfIsolationScreenOnVoid(remainingDaysInIsolation: Int) {
        showIsolationState(
            hasCloseToolbar = true,
            remainingDaysInIsolation = remainingDaysInIsolation,
            iconResource = R.drawable.ic_isolation_book_test,
            stateText = R.string.state_test_void_info,
            actionButtonStringResource = R.string.book_free_test,
            exposureLinksVisible = false,
            onlineServiceLinkText = R.string.test_result_void_continue_self_isolate_nhs_guidance_label,
            onlineServiceLinkUrl = R.string.url_nhs_guidance,
            paragraphResources = intArrayOf(
                R.string.test_result_void_continue_self_isolate_explanation
            )
        )
    }

    private fun showContinueToSelfIsolationScreenOnNegativeAfterPositiveOrSymptomatic(remainingDaysInIsolation: Int) {
        showIsolationState(
            remainingDaysInIsolation = remainingDaysInIsolation,
            stateText = R.string.state_test_positive_then_negative_info,
            exposureLinksVisible = false,
            paragraphResources = intArrayOf(
                R.string.test_result_positive_then_negative_explanation
            )
        )
    }

    private fun showSelfIsolateScreenOnPositive(remainingDaysInIsolation: Int) {
        showIsolationState(
            remainingDaysInIsolation = remainingDaysInIsolation,
            selfIsolationLabel = R.string.self_isolate_for,
            stateText = R.string.state_test_positive_info,
            exposureLinksVisible = true,
            paragraphResources = intArrayOf(
                R.string.test_result_negative_then_positive_continue_explanation,
                R.string.exposure_faqs_title
            )
        )
    }

    private fun showSelfIsolateScreenOnPositiveAndOrderTest(remainingDaysInIsolation: Int) {
        showIsolationState(
            hasCloseToolbar = true,
            remainingDaysInIsolation = remainingDaysInIsolation,
            iconResource = R.drawable.ic_isolation_book_test,
            stateText = R.string.state_test_positive_and_book_test_info,
            stateColor = R.color.amber,
            selfIsolationLabel = R.string.self_isolate_for,
            additionalIsolationInfoText = R.string.test_result_positive_self_isolate_and_book_test_title_3,
            actionButtonStringResource = R.string.book_follow_up_test,
            exposureLinksVisible = true,
            paragraphResources = intArrayOf(
                R.string.test_result_positive_self_isolate_and_book_test_explanation_1,
                R.string.exposure_faqs_title
            )
        )
    }
    //endregion

    //region Good news states
    private fun showGoodNewsState(
        hasCloseToolbar: Boolean = false,
        hasGoodNewsLink: Boolean = true,
        @DrawableRes iconResource: Int? = R.drawable.ic_isolation_expired_or_over,
        @StringRes titleStringResource: Int = R.string.expiration_notification_title,
        @StringRes subtitleStringResource: Int,
        @StringRes goodNewsInfoViewResource: Int = R.string.test_result_no_self_isolation_description,
        @StringRes actionButtonStringResource: Int = R.string.continue_button,
        @StringRes vararg paragraphResources: Int = intArrayOf(R.string.for_further_advice_visit)
    ) = with(binding.goodNewsContainer) {
        if (hasCloseToolbar) setCloseToolbar()

        goodNewsOnlineServiceLink.isVisible = hasGoodNewsLink

        root.visible()
        binding.isolationRequestContainer.root.gone()

        if (iconResource == null) {
            goodNewsIcon.gone()
        } else {
            goodNewsIcon.setImageResource(iconResource)
        }

        with(getString(titleStringResource)) {
            goodNewsTitle.text = this
            title = this
        }

        goodNewsSubtitle.text = getString(subtitleStringResource)
        goodNewsParagraphContainer.addAllParagraphs(paragraphResources.map { getString(it) })
        goodNewsActionButton.text = getString(actionButtonStringResource)
        goodNewsInfoView.stateText = getString(goodNewsInfoViewResource)

        goodNewsActionButton.setOnSingleClickListener {
            viewModel.onActionButtonClicked()
        }
    }

    private fun showDoNotHaveToSelfIsolateScreenOnPositive() {
        showGoodNewsState(
            titleStringResource = R.string.test_result_your_test_result,
            subtitleStringResource = R.string.test_result_positive_no_self_isolation_subtitle
        )
    }

    private fun showDoNotHaveToSelfIsolateScreenOnNegative() {
        showGoodNewsState(
            subtitleStringResource = R.string.test_result_negative_no_self_isolation_subtitle_text
        )
    }

    private fun showAreNotIsolatingScreenOnNegative() {
        showGoodNewsState(
            subtitleStringResource = R.string.test_result_negative_already_not_in_isolation_subtitle
        )
    }

    private fun showAreNotIsolatingScreenOnVoid() {
        showGoodNewsState(
            hasCloseToolbar = true,
            titleStringResource = R.string.test_result_your_test_result,
            subtitleStringResource = R.string.test_result_void_already_not_in_isolation_subtitle,
            actionButtonStringResource = R.string.book_free_test
        )
    }
    //endregion

    //region PLOD states
    private fun showContinueWithCurrentStateScreenOnPlod() {
        showGoodNewsState(
            hasCloseToolbar = true,
            titleStringResource = R.string.test_result_plod_title,
            subtitleStringResource = R.string.test_result_plod_subtitle,
            goodNewsInfoViewResource = R.string.test_result_plod_description,
            actionButtonStringResource = R.string.back_to_home,
            hasGoodNewsLink = false,
            iconResource = null,
            paragraphResources = intArrayOf(R.string.test_result_plod_info)
        )
    }
    //endregion

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1339
        const val REQUEST_CODE_SHARE_KEYS = 1534
    }
}
