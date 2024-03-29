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
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityTestResultBinding
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShareKeysInformationActivity
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.Finish
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.NavigateToOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.NavigationEvent.NavigateToShareKeys
import uk.nhs.nhsx.covid19.android.app.testordering.BaseTestResultViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.FollowUpTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.NoTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.RegularTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.Ignore
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PlodWillContinueWithCurrentState
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
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
        viewModel.fetchCountry()
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

        fetchViewState()
    }

    private fun fetchViewState() {
        viewModel.viewState().observe(this) { viewState ->
            when (viewState.mainState) {
                NegativeNotInIsolation ->
                    showAreNotIsolatingScreenOnNegative(viewState.country)
                NegativeWillBeInIsolation ->
                    showContinueToSelfIsolationScreenOnNegative(viewState.remainingDaysInIsolation)
                NegativeWontBeInIsolation ->
                    showDoNotHaveToSelfIsolateScreenOnNegative()
                PositiveContinueIsolation ->
                    showContinueToSelfIsolationScreenOnPositive(viewState.remainingDaysInIsolation, viewState.country)
                PositiveContinueIsolationNoChange ->
                    showContinueToSelfIsolationScreenOnPositiveAndNoChange(
                        viewState.remainingDaysInIsolation,
                        viewState.country
                    )
                PositiveWillBeInIsolation ->
                    showWillBeInIsolationOnPositive(viewState)
                PositiveWontBeInIsolation ->
                    showDoNotHaveToSelfIsolateScreenOnPositive(viewState.country)
                NegativeAfterPositiveOrSymptomaticWillBeInIsolation ->
                    showContinueToSelfIsolationScreenOnNegativeAfterPositiveOrSymptomatic(
                        viewState.remainingDaysInIsolation,
                        viewState.country
                    )
                VoidNotInIsolation ->
                    showAreNotIsolatingScreenOnVoid(viewState.country)
                VoidWillBeInIsolation ->
                    showContinueToSelfIsolationScreenOnVoid(viewState.remainingDaysInIsolation, viewState.country)
                PlodWillContinueWithCurrentState ->
                    showContinueWithCurrentStateScreenOnPlod(viewState.country)
                Ignore -> finish()
            }
            updateActionButton(viewState.acknowledgementCompletionActions, viewState.mainState, viewState.country)
        }
    }

    private fun showWillBeInIsolationOnPositive(viewState: ViewState) {
        if (viewState.acknowledgementCompletionActions.suggestBookTest == FollowUpTest && !viewState.acknowledgementCompletionActions.shouldAllowKeySubmission)
            showSelfIsolateScreenOnPositiveAndOrderTest(viewState.remainingDaysInIsolation, viewState.country)
        else
            showSelfIsolateScreenOnPositive(viewState.remainingDaysInIsolation, viewState.country)
    }

    private fun updateActionButton(
        acknowledgementCompletionActions: AcknowledgementCompletionActions,
        mainState: TestResultViewState,
        country: PostCodeDistrict?
    ) = with(binding) {
        val showBackToHomeIfNoSpecificAction =
            mainState in listOf(
                PlodWillContinueWithCurrentState,
                NegativeWillBeInIsolation
            )

        val showBackToHomeIfNoBookTestAction = mainState in listOf(
            VoidWillBeInIsolation, VoidNotInIsolation
        )

        val actionButtonStringResource = when {
            acknowledgementCompletionActions.shouldAllowKeySubmission -> getButtonLabelWhenKeySubmissionIsAllowed(
                mainState,
                country
            )

            acknowledgementCompletionActions.suggestBookTest == FollowUpTest -> R.string.book_follow_up_test
            acknowledgementCompletionActions.suggestBookTest == RegularTest -> if (showBackToHomeIfNoBookTestAction) R.string.void_test_results_primary_button_title else R.string.book_free_test
            acknowledgementCompletionActions.suggestBookTest == NoTest -> if (showBackToHomeIfNoSpecificAction) R.string.back_to_home else R.string.continue_button
            else -> R.string.back_to_home
        }
        with(goodNewsContainer) {
            goodNewsActionButton.text = getString(actionButtonStringResource)
            goodNewsActionButton.setOnSingleClickListener {
                viewModel.onActionButtonClicked()
            }
        }
        with(isolationRequestContainer) {
            isolationRequestActionButton.text = getString(actionButtonStringResource)
            isolationRequestActionButton.setOnSingleClickListener { viewModel.onActionButtonClicked() }
        }
    }

    private fun getButtonLabelWhenKeySubmissionIsAllowed(
        mainState: TestResultViewState,
        country: PostCodeDistrict?
    ): Int {
        return if (mainState == PositiveWillBeInIsolation && country == ENGLAND) {
            R.string.index_case_isolation_advice_primary_button_title_england
        } else {
            R.string.continue_button
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
        exposureLinksVisible: Boolean,
        @StringRes furtherAdviceText: Int = R.string.for_further_advice_visit,
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

            isolationFurtherAdviceTextView.text = getString(furtherAdviceText)

            isolationRequestOnlineServiceLink.setDisplayText(onlineServiceLinkText)
            isolationRequestOnlineServiceLink.setLinkUrl(onlineServiceLinkUrl)
        }
    }

    private fun showIsolationAdviceForEngland(
        hasCloseToolbar: Boolean = false,
        @DrawableRes iconResource: Int = R.drawable.ic_isolation_continue,
        @StringRes stateText: Int = R.string.index_case_isolation_advice_information_box_description_england,
        @ColorRes stateColor: Int = R.color.amber,
        @StringRes title: Int = R.string.index_case_isolation_advice_heading_title_england,
        @StringRes onlineServiceLinkText: Int = R.string.index_case_isolation_advice_nhs_onilne_link_button_england,
        @StringRes onlineServiceLinkUrl: Int = R.string.url_nhs_111_online,
        @StringRes paragraphText: Int = R.string.index_case_isolation_advice_body_england
    ) = with(binding) {
        if (hasCloseToolbar) {
            setCloseToolbar()
        }
        goodNewsContainer.root.gone()
        with(isolationRequestContainer) {

            root.visible()
            exposureFaqsLink.visible()

            isolationRequestImage.setImageResource(iconResource)

            val titleText = getString(title)
            isolationRequestTitle1.text = titleText
            isolationRequestTitle2.gone()
            isolationRequestTitle3.gone()

            accessibilityContainer.contentDescription = titleText
            accessibilityContainer.setUpAccessibilityHeading()

            isolationRequestContainer.isolationRequestInfoView.apply {
                this.stateText = getString(stateText)
                this.stateColor = getColor(stateColor)
            }

            isolationRequestParagraphContainer.setRawText(getString(paragraphText))

            isolationRequestOnlineServiceLink.setDisplayText(onlineServiceLinkText)
            isolationRequestOnlineServiceLink.setLinkUrl(onlineServiceLinkUrl)
        }
    }

    private fun showContinueToSelfIsolationScreenOnPositive(remainingDaysInIsolation: Int, country: PostCodeDistrict?) {
        if (country == ENGLAND) {
            showIsolationState(
                hasCloseToolbar = false,
                selfIsolationLabel = R.string.index_case_continue_isolation_advice_heading_title_england,
                stateText = R.string.state_test_positive_continue_isolation_info_england,
                remainingDaysInIsolation = remainingDaysInIsolation,
                exposureLinksVisible = false,
                onlineServiceLinkText = R.string.index_case_continue_isolation_advice_nhs_onilne_link_button_england,
                onlineServiceLinkUrl = R.string.url_nhs_111_online,
                paragraphResources = intArrayOf(R.string.index_case_continue_isolation_advice_body_england)
            )
        } else {
            showIsolationState(
                remainingDaysInIsolation = remainingDaysInIsolation,
                exposureLinksVisible = false,
                paragraphResources = intArrayOf(R.string.test_result_positive_continue_self_isolate_explanation_1)
            )
        }
    }

    private fun showContinueToSelfIsolationScreenOnPositiveAndNoChange(
        remainingDaysInIsolation: Int,
        country: PostCodeDistrict?
    ) {
        if (country == ENGLAND) {
            showIsolationState(
                hasCloseToolbar = false,
                selfIsolationLabel = R.string.index_case_continue_isolation_advice_heading_title_england,
                stateText = R.string.state_test_positive_continue_isolation_info_england,
                remainingDaysInIsolation = remainingDaysInIsolation,
                exposureLinksVisible = false,
                onlineServiceLinkText = R.string.index_case_continue_isolation_advice_nhs_onilne_link_button_england,
                onlineServiceLinkUrl = R.string.url_nhs_111_online,
                paragraphResources = intArrayOf(R.string.index_case_continue_isolation_advice_body_england)
            )
        } else {
            showIsolationState(
                remainingDaysInIsolation = remainingDaysInIsolation,
                exposureLinksVisible = true,
                paragraphResources = intArrayOf(
                    R.string.test_result_positive_continue_self_isolate_no_change_explanation_1,
                    R.string.exposure_faqs_title
                )
            )
        }
    }

    private fun showContinueToSelfIsolationScreenOnNegative(remainingDaysInIsolation: Int) {
        showIsolationState(
            remainingDaysInIsolation = remainingDaysInIsolation,
            exposureLinksVisible = false,
            stateText = R.string.state_test_negative_info,
            stateColor = R.color.amber,
            onlineServiceLinkText = R.string.test_result_negative_continue_self_isolate_nhs_guidance_label,
            onlineServiceLinkUrl = R.string.url_nhs_guidance,
            paragraphResources = intArrayOf(
                R.string.test_result_negative_continue_self_isolate_explanation
            )
        )
    }

    private fun showContinueToSelfIsolationScreenOnVoid(remainingDaysInIsolation: Int, country: PostCodeDistrict?) {
        if (country == ENGLAND) {
            showIsolationState(
                hasCloseToolbar = true,
                remainingDaysInIsolation = remainingDaysInIsolation,
                iconResource = R.drawable.ic_isolation_book_test,
                stateText = R.string.state_test_void_info,
                stateColor = R.color.amber,
                selfIsolationLabel = R.string.test_result_void_continue_self_isolate_title_1,
                exposureLinksVisible = false,
                furtherAdviceText = R.string.test_result_void_continue_self_isolate_advice,
                onlineServiceLinkText = R.string.test_result_void_continue_self_isolate_nhs_guidance_label,
                onlineServiceLinkUrl = R.string.url_nhs_guidance,
                paragraphResources = intArrayOf(
                    R.string.test_result_void_continue_self_isolate_explanation
                )
            )
        } else {
            showIsolationState(
                hasCloseToolbar = true,
                remainingDaysInIsolation = remainingDaysInIsolation,
                iconResource = R.drawable.ic_isolation_book_test,
                stateText = R.string.state_test_void_info_wls,
                stateColor = R.color.amber,
                selfIsolationLabel = R.string.test_result_void_continue_self_isolate_title_1_wls,
                exposureLinksVisible = false,
                furtherAdviceText = R.string.test_result_void_continue_self_isolate_advice_wls,
                onlineServiceLinkText = R.string.test_result_void_continue_self_isolate_nhs_guidance_label_wls,
                onlineServiceLinkUrl = R.string.url_nhs_guidance,
                paragraphResources = intArrayOf(
                    R.string.test_result_void_continue_self_isolate_explanation_wls
                )
            )
        }
    }

    private fun showContinueToSelfIsolationScreenOnNegativeAfterPositiveOrSymptomatic(
        remainingDaysInIsolation: Int,
        country: PostCodeDistrict?
    ) {
        if (country == ENGLAND) {
            showIsolationState(
                selfIsolationLabel = R.string.test_result_positive_then_negative_continue_self_isolation_title_1,
                remainingDaysInIsolation = remainingDaysInIsolation,
                stateText = R.string.state_test_positive_then_negative_info,
                exposureLinksVisible = false,
                paragraphResources = intArrayOf(R.string.test_result_positive_then_negative_explanation),
                furtherAdviceText = R.string.test_result_positive_then_negative_continue_self_isolation_for_further_advice_visit,
                onlineServiceLinkText = R.string.nhs_111_online_service
            )
        } else {
            showIsolationState(
                selfIsolationLabel = R.string.test_result_positive_then_negative_continue_self_isolation_title_1_wls,
                remainingDaysInIsolation = remainingDaysInIsolation,
                stateText = R.string.state_test_positive_then_negative_info_wls,
                exposureLinksVisible = false,
                paragraphResources = intArrayOf(R.string.test_result_positive_then_negative_explanation_wls),
                furtherAdviceText = R.string.test_result_positive_then_negative_continue_self_isolation_for_further_advice_visit_wls,
                onlineServiceLinkText = R.string.nhs_111_online_service_wales
            )
        }
    }

    private fun showSelfIsolateScreenOnPositive(remainingDaysInIsolation: Int, country: PostCodeDistrict?) {
        if (country != null && country == PostCodeDistrict.ENGLAND) {
            showIsolationAdviceForEngland(
                stateColor = R.color.error_red
            )
        } else {
            showIsolationState(
                iconResource = R.drawable.ic_isolation_book_test,
                remainingDaysInIsolation = remainingDaysInIsolation,
                selfIsolationLabel = R.string.try_to_stay_at_home_for_after_positive_test_wales,
                stateText = R.string.infobox_after_positive_test_wales,
                stateColor = R.color.error_red,
                exposureLinksVisible = false,
                paragraphResources = intArrayOf(R.string.test_result_negative_then_positive_continue_explanation)
            )
        }
    }

    private fun showSelfIsolateScreenOnPositiveAndOrderTest(remainingDaysInIsolation: Int, country: PostCodeDistrict?) {
        if (country != null && country == PostCodeDistrict.ENGLAND) {
            showIsolationAdviceForEngland(
                hasCloseToolbar = true
            )
        } else {
            showIsolationState(
                hasCloseToolbar = true,
                remainingDaysInIsolation = remainingDaysInIsolation,
                iconResource = R.drawable.ic_isolation_book_test,
                stateText = R.string.state_test_positive_and_book_test_info,
                stateColor = R.color.amber,
                selfIsolationLabel = R.string.self_isolate_for,
                additionalIsolationInfoText = R.string.test_result_positive_self_isolate_and_book_test_title_3,
                exposureLinksVisible = true,
                paragraphResources = intArrayOf(
                    R.string.test_result_positive_self_isolate_and_book_test_explanation_1,
                    R.string.exposure_faqs_title
                )
            )
        }
    }
    //endregion

    //region Good news states
    private fun showGoodNewsState(
        hasCloseToolbar: Boolean = false,
        hasGoodNewsLink: Boolean = true,
        @DrawableRes iconResource: Int? = R.drawable.ic_elbow_bump,
        @StringRes titleStringResource: Int,
        @StringRes subtitleStringResource: Int,
        @StringRes goodNewsInfoViewResource: Int,
        @StringRes vararg paragraphResources: Int = intArrayOf(R.string.for_further_advice_visit),
        @StringRes onlineServiceLinkText: Int = R.string.nhs_111_online_service,
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
        goodNewsInfoView.stateText = getString(goodNewsInfoViewResource)
        goodNewsOnlineServiceLink.text = getString(onlineServiceLinkText)
    }

    private fun showDoNotHaveToSelfIsolateScreenOnPositive(country: PostCodeDistrict?) {
        if (country == ENGLAND) {
            showGoodNewsState(
                titleStringResource = R.string.test_result_positive_no_self_isolation_title,
                subtitleStringResource = R.string.test_result_positive_no_self_isolation_subtitle,
                goodNewsInfoViewResource = R.string.test_result_no_self_isolation_description,
                paragraphResources = intArrayOf(R.string.test_result_no_self_isolation_advice),
                onlineServiceLinkText = R.string.nhs_111_online_service
            )
        } else {
            showGoodNewsState(
                titleStringResource = R.string.test_result_positive_no_self_isolation_title_wls,
                subtitleStringResource = R.string.test_result_positive_no_self_isolation_subtitle_wls,
                goodNewsInfoViewResource = R.string.test_result_no_self_isolation_description_wls,
                paragraphResources = intArrayOf(R.string.test_result_no_self_isolation_advice_wls),
                onlineServiceLinkText = R.string.nhs_111_online_service_wales
            )
        }
    }

    private fun showDoNotHaveToSelfIsolateScreenOnNegative() {
        showGoodNewsState(
            titleStringResource = R.string.negative_test_result_good_news_title,
            subtitleStringResource = R.string.test_result_negative_no_self_isolation_subtitle_text,
            goodNewsInfoViewResource = R.string.negative_test_result_no_self_isolation_description
        )
    }

    private fun showAreNotIsolatingScreenOnNegative(country: PostCodeDistrict?) {
        if (country == ENGLAND) {
            showGoodNewsState(
                titleStringResource = R.string.negative_test_result_good_news_title,
                subtitleStringResource = R.string.test_result_negative_already_not_in_isolation_subtitle,
                goodNewsInfoViewResource = R.string.negative_test_result_no_self_isolation_description,
                paragraphResources = intArrayOf(R.string.test_result_negative_already_not_in_isolation_advice),
                onlineServiceLinkText = R.string.nhs_111_online_service
            )
        } else {
            showGoodNewsState(
                titleStringResource = R.string.negative_test_result_good_news_title_wls,
                subtitleStringResource = R.string.test_result_negative_already_not_in_isolation_subtitle_wls,
                goodNewsInfoViewResource = R.string.negative_test_result_no_self_isolation_description_wls,
                paragraphResources = intArrayOf(R.string.test_result_negative_already_not_in_isolation_advice_wls),
                onlineServiceLinkText = R.string.nhs_111_online_service_wales
            )
        }
    }

    private fun showAreNotIsolatingScreenOnVoid(country: PostCodeDistrict?) {
        if (country == ENGLAND) {
            showGoodNewsState(
                hasCloseToolbar = true,
                titleStringResource = R.string.test_result_your_test_result,
                subtitleStringResource = R.string.test_result_void_already_not_in_isolation_subtitle,
                goodNewsInfoViewResource = R.string.void_test_result_no_self_isolation_warning,
                paragraphResources = intArrayOf(R.string.void_test_result_no_self_isolation_advice),
                onlineServiceLinkText = R.string.nhs_111_online_service
            )
        } else {
            showGoodNewsState(
                hasCloseToolbar = true,
                titleStringResource = R.string.test_result_your_test_result_wls,
                subtitleStringResource = R.string.test_result_void_already_not_in_isolation_subtitle_wls,
                goodNewsInfoViewResource = R.string.void_test_result_no_self_isolation_warning_wls,
                paragraphResources = intArrayOf(R.string.void_test_result_no_self_isolation_advice_wls),
                onlineServiceLinkText = R.string.nhs_111_online_service_wales
            )
        }
    }
    //endregion

    //region PLOD states
    private fun showContinueWithCurrentStateScreenOnPlod(country: PostCodeDistrict?) {
        if (country == ENGLAND) {
            showGoodNewsState(
                hasCloseToolbar = true,
                titleStringResource = R.string.test_result_plod_title,
                subtitleStringResource = R.string.test_result_plod_subtitle,
                goodNewsInfoViewResource = R.string.test_result_plod_description,
                hasGoodNewsLink = false,
                iconResource = null,
                paragraphResources = intArrayOf(R.string.test_result_plod_info)
            )
        } else {
            showGoodNewsState(
                hasCloseToolbar = true,
                titleStringResource = R.string.test_result_plod_title_wls,
                subtitleStringResource = R.string.test_result_plod_subtitle_wls,
                goodNewsInfoViewResource = R.string.test_result_plod_description_wls,
                hasGoodNewsLink = false,
                iconResource = null,
                paragraphResources = intArrayOf(R.string.test_result_plod_info_wls)
            )
        }
    }
    //endregion

    companion object {
        const val REQUEST_CODE_ORDER_A_TEST = 1339
        const val REQUEST_CODE_SHARE_KEYS = 1534
    }
}
