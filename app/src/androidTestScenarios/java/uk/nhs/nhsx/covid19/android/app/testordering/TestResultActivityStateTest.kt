package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertInternalBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.FollowUpTest
import uk.nhs.nhsx.covid19.android.app.testordering.BookTestOption.NoTest
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

class TestResultActivityStateTest : EspressoTest() {
    val context = testAppContext.app
    private val testResultRobot = TestResultRobot(context)
    private val browserRobot = BrowserRobot()

    @After
    fun cleanUp() {
        MockTestResultViewModel.currentOptions =
            MockTestResultViewModel.currentOptions.copy(
                useMock = false
            )
    }

    private fun getString(resourceId: Int): String =
        context.resources.getString(resourceId)

    private fun setState(
        state: TestResultViewState,
        acknowledgementCompletionActions: AcknowledgementCompletionActions,
        days: Int = -1,
        country: PostCodeDistrict = WALES
    ) {
        MockTestResultViewModel.currentOptions = MockTestResultViewModel.currentOptions.copy(
            useMock = true,
            viewState = state,
            actions = acknowledgementCompletionActions,
            remainingDaysInIsolation = days,
            country = country
        )

        startTestActivity<TestResultActivity>()
    }

    private fun checkGoodNewsState(
        state: TestResultViewState,
        country: PostCodeDistrict = WALES,
        hasCloseToolbar: Boolean,
        @DrawableRes iconDrawableRes: Int?,
        @StringRes titleStringResource: Int = -1,
        @StringRes subtitleStringResource: Int,
        @StringRes actionButtonStringResource: Int,
        @StringRes vararg paragraphResources: Int,
        @StringRes goodNewsInfoStringResource: Int,
        hasGoodNewsLink: Boolean,
        @StringRes onlineServiceLinkText: Int = R.string.nhs_111_online_service,
    ) {
        setState(
            state = state,
            acknowledgementCompletionActions = AcknowledgementCompletionActions(
                suggestBookTest = NoTest,
                shouldAllowKeySubmission = false
            ),
            country = country
        )

        testResultRobot.apply {
            if (hasCloseToolbar) checkHasCloseToolbarOption() else checkNoCloseToolbarOption()
            checkGoodNewsVisibility(true)
            checkIsolationRequestVisibility(false)

            checkGoodNewsIcon(iconDrawableRes)
            checkGoodNewsTitleIsVisible()
            if (titleStringResource > 0) checkGoodNewsTitleStringResource(titleStringResource)
            checkGoodNewsSubtitleStringResource(subtitleStringResource)
            checkGoodNewsInfoState(goodNewsInfoStringResource)
            checkGoodNewsParagraphContainStringResources(*paragraphResources)

            checkGoodNewsActionButtonTextStringResource(actionButtonStringResource)
            checkExposureFaqsLinkNotVisible()
            checkGoodNewsLinkVisibility(hasGoodNewsLink)
            if (hasGoodNewsLink) checkGoodNewsOnlineServiceLinkText(onlineServiceLinkText)
        }

        if (iconDrawableRes != null) {
            setScreenOrientation(LANDSCAPE)
            waitFor { testResultRobot.checkGoodNewsIconVisibility(false) }
            setScreenOrientation(PORTRAIT)
            waitFor { testResultRobot.checkGoodNewsIconVisibility(true) }
        }
    }

    private fun checkIsolationState(
        state: TestResultViewState,
        country: PostCodeDistrict = WALES,
        days: Int,
        hasCloseToolbar: Boolean,
        @DrawableRes iconDrawableRes: Int,
        @StringRes isolationRequestInfoStringResource: Int,
        @StringRes isolationRequestInfoColorResource: Int,
        @StringRes title1: Int,
        title3Visible: Boolean,
        @StringRes title3: Int = -1,
        @StringRes actionButtonStringResource: Int,
        exposureLinksVisible: Boolean,
        @StringRes furtherAdviceStringResource: Int = R.string.for_further_advice_visit,
        @StringRes onlineServiceLinkText: Int,
        @StringRes onlineServiceLinkUrl: Int,
        @StringRes vararg paragraphResources: Int,
        acknowledgementCompletionActions: AcknowledgementCompletionActions = AcknowledgementCompletionActions(
            suggestBookTest = NoTest,
            shouldAllowKeySubmission = false
        )
    ) {
        setState(
            state = state,
            acknowledgementCompletionActions = acknowledgementCompletionActions,
            days = days,
            country = country
        )

        val title2 = "$days day${if (days > 1) "s" else ""}"

        testResultRobot.apply {
            if (hasCloseToolbar) checkHasCloseToolbarOption() else checkNoCloseToolbarOption()
            checkGoodNewsVisibility(false)
            checkIsolationRequestVisibility(true)

            checkIsolationRequestImageIs(iconDrawableRes)
            checkIsolationRequestTitle1Is(title1)
            checkIsolationRequestTitle2Is(title2)
            checkIsolationRequestTitle3Visibility(title3Visible)

            val testAccessibilityContainerContentDescription =
                "${getString(title1)} $title2 ${if (title3Visible) getString(title3) else ""}"
            checkAccessibilityContainerContentDescription(testAccessibilityContainerContentDescription)

            checkAccessibilityContainerIsAccessibilityHeading()

            checkIsolationRequestInfoState(isolationRequestInfoStringResource, isolationRequestInfoColorResource)

            checkIsolationRequestTitle1IsNotAccessibilityHeading()
            checkIsolationRequestTitle2IsNotAccessibilityHeading()
            checkIsolationRequestTitle3IsNotAccessibilityHeading()
            checkIsolationRequestTitle1HasNullContentDescription()
            checkIsolationRequestTitle2HasNullContentDescription()
            checkIsolationRequestTitle3HasNullContentDescription()

            if (paragraphResources.isNotEmpty())
                checkIsolationRequestParagraphContainerContains(*paragraphResources)

            if (exposureLinksVisible) checkExposureFaqsLinkVisible()
            else checkExposureFaqsLinkNotVisible()

            checkFurtherAdviceStringResource(furtherAdviceStringResource)

            checkOnlineServiceLinkText(onlineServiceLinkText)

            runWithFeatureEnabled(USE_WEB_VIEW_FOR_INTERNAL_BROWSER) {
                assertInternalBrowserIsOpened(getString(onlineServiceLinkUrl)) {
                    clickServiceLink()
                    waitFor { browserRobot.checkActivityIsDisplayed() }
                    browserRobot.clickCloseButton()
                }
            }

            checkIsolationRequestActionButton(actionButtonStringResource)
        }
    }

    @Test
    fun showNegativeNotInIsolation_england() {
        checkGoodNewsState(
            state = NegativeNotInIsolation,
            country = ENGLAND,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.negative_test_result_good_news_title,
            subtitleStringResource = R.string.test_result_negative_already_not_in_isolation_subtitle,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.test_result_negative_already_not_in_isolation_advice),
            goodNewsInfoStringResource = R.string.negative_test_result_no_self_isolation_description,
            hasGoodNewsLink = true,
            onlineServiceLinkText = R.string.nhs_111_online_service
        )
    }
    @Test
    fun showNegativeNotInIsolation_wales() {
        checkGoodNewsState(
            state = NegativeNotInIsolation,
            country = WALES,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.negative_test_result_good_news_title_wls,
            subtitleStringResource = R.string.test_result_negative_already_not_in_isolation_subtitle_wls,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.test_result_negative_already_not_in_isolation_advice_wls),
            goodNewsInfoStringResource = R.string.negative_test_result_no_self_isolation_description_wls,
            hasGoodNewsLink = true,
            onlineServiceLinkText = R.string.nhs_111_online_service_wales
        )
    }

    @Test
    fun showNegativeWillBeInIsolation() {
        checkIsolationState(
            state = NegativeWillBeInIsolation,
            days = 3,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_negative_info,
            isolationRequestInfoColorResource = R.color.amber,
            title1 = R.string.test_result_positive_continue_self_isolation_title_1,
            actionButtonStringResource = R.string.back_to_home,
            title3Visible = false,
            exposureLinksVisible = false,
            onlineServiceLinkText = R.string.test_result_negative_continue_self_isolate_nhs_guidance_label,
            onlineServiceLinkUrl = R.string.url_nhs_guidance,
            acknowledgementCompletionActions = AcknowledgementCompletionActions(
                suggestBookTest = NoTest,
                shouldAllowKeySubmission = false
            )
        )
    }

    @Test
    fun showNegativeWontBeInIsolation() {
        checkGoodNewsState(
            state = NegativeWontBeInIsolation,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.expiration_notification_title,
            subtitleStringResource = R.string.test_result_negative_no_self_isolation_subtitle_text,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.for_further_advice_visit),
            goodNewsInfoStringResource = R.string.negative_test_result_no_self_isolation_description,
            hasGoodNewsLink = true
        )
    }

    @Test
    fun showPositiveContinueIsolation_wales() {
        checkIsolationState(
            state = PositiveContinueIsolation,
            days = 4,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_positive_info,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.test_result_positive_continue_self_isolation_title_1,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(
                R.string.test_result_positive_continue_self_isolate_explanation_1
            )
        )
    }

    @Test
    fun showPositiveContinueIsolation_england() {
        checkIsolationState(
            state = PositiveContinueIsolation,
            country = ENGLAND,
            days = 4,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_positive_continue_isolation_info_england,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.index_case_continue_isolation_advice_heading_title_england,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(
                R.string.index_case_continue_isolation_advice_body_england
            )
        )
    }

    @Test
    fun showPositiveContinueIsolationNoChange_wales() {
        checkIsolationState(
            state = PositiveContinueIsolationNoChange,
            days = 5,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_positive_info,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.test_result_positive_continue_self_isolation_title_1,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = true,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(R.string.test_result_positive_continue_self_isolate_no_change_explanation_1)
        )
    }

    @Test
    fun showPositiveContinueIsolationNoChange_england() {
        checkIsolationState(
            state = PositiveContinueIsolationNoChange,
            country = ENGLAND,
            days = 5,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_positive_continue_isolation_info_england,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.index_case_continue_isolation_advice_heading_title_england,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(R.string.index_case_continue_isolation_advice_body_england)
        )
    }

    @Test
    fun showPositiveWillBeInIsolation() {
        checkIsolationState(
            state = PositiveWillBeInIsolation,
            days = 6,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_book_test,
            isolationRequestInfoStringResource = R.string.infobox_after_positive_test_wales,
            isolationRequestInfoColorResource = R.color.amber,
            title1 = R.string.try_to_stay_at_home_for_after_positive_test_wales,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(
                R.string.test_result_negative_then_positive_continue_explanation
            )
        )
    }

    @Test
    fun showPositiveWontBeInIsolation_england() {
        checkGoodNewsState(
            state = PositiveWontBeInIsolation,
            country = ENGLAND,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.test_result_positive_no_self_isolation_title,
            subtitleStringResource = R.string.test_result_positive_no_self_isolation_subtitle,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.test_result_no_self_isolation_advice),
            goodNewsInfoStringResource = R.string.test_result_no_self_isolation_description,
            hasGoodNewsLink = true,
            onlineServiceLinkText = R.string.nhs_111_online_service
        )
    }

    @Test
    fun showPositiveWontBeInIsolation_wales() {
        checkGoodNewsState(
            state = PositiveWontBeInIsolation,
            country = WALES,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.test_result_positive_no_self_isolation_title_wls,
            subtitleStringResource = R.string.test_result_positive_no_self_isolation_subtitle_wls,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.test_result_no_self_isolation_advice_wls),
            goodNewsInfoStringResource = R.string.test_result_no_self_isolation_description_wls,
            hasGoodNewsLink = true,
            onlineServiceLinkText = R.string.nhs_111_online_service_wales
        )
    }

    @Test
    fun showNegativeAfterPositiveOrSymptomaticWillBeInIsolation_england() {
        checkIsolationState(
            state = NegativeAfterPositiveOrSymptomaticWillBeInIsolation,
            country = ENGLAND,
            days = 7,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_positive_then_negative_info,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.test_result_positive_then_negative_continue_self_isolation_title_1,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            furtherAdviceStringResource = R.string.test_result_positive_then_negative_continue_self_isolation_for_further_advice_visit,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(R.string.test_result_positive_then_negative_explanation)
        )
    }

    @Test
    fun showNegativeAfterPositiveOrSymptomaticWillBeInIsolation_wales() {
        checkIsolationState(
            state = NegativeAfterPositiveOrSymptomaticWillBeInIsolation,
            country = WALES,
            days = 7,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_positive_then_negative_info_wls,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.test_result_positive_then_negative_continue_self_isolation_title_1_wls,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            furtherAdviceStringResource = R.string.test_result_positive_then_negative_continue_self_isolation_for_further_advice_visit_wls,
            onlineServiceLinkText = R.string.nhs_111_online_service_wales,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(R.string.test_result_positive_then_negative_explanation_wls)
        )
    }

    @Test
    fun showPositiveWillBeInIsolationAndOrderTest() {
        checkIsolationState(
            state = PositiveWillBeInIsolation,
            days = 1,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_isolation_book_test,
            isolationRequestInfoStringResource = R.string.state_test_positive_and_book_test_info,
            isolationRequestInfoColorResource = R.color.amber,
            title1 = R.string.self_isolate_for,
            title3Visible = true,
            title3 = R.string.test_result_positive_self_isolate_and_book_test_title_3,
            actionButtonStringResource = R.string.book_follow_up_test,
            exposureLinksVisible = true,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(R.string.test_result_positive_self_isolate_and_book_test_explanation_1),
            acknowledgementCompletionActions =
            AcknowledgementCompletionActions(
                suggestBookTest = FollowUpTest,
                shouldAllowKeySubmission = false
            )
        )
    }

    @Test
    fun showVoidNotInIsolation_england() {
        checkGoodNewsState(
            state = VoidNotInIsolation,
            country = ENGLAND,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.test_result_your_test_result,
            subtitleStringResource = R.string.test_result_void_already_not_in_isolation_subtitle,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.void_test_result_no_self_isolation_advice),
            goodNewsInfoStringResource = R.string.void_test_result_no_self_isolation_warning,
            hasGoodNewsLink = true,
            onlineServiceLinkText = R.string.nhs_111_online_service
        )
    }

    @Test
    fun showVoidNotInIsolation_wales() {
        checkGoodNewsState(
            state = VoidNotInIsolation,
            country = WALES,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.test_result_your_test_result_wls,
            subtitleStringResource = R.string.test_result_void_already_not_in_isolation_subtitle_wls,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.void_test_result_no_self_isolation_advice_wls),
            goodNewsInfoStringResource = R.string.void_test_result_no_self_isolation_warning_wls,
            hasGoodNewsLink = true,
            onlineServiceLinkText = R.string.nhs_111_online_service_wales
        )
    }

    @Test
    fun showVoidWillBeInIsolation_england() {
        checkIsolationState(
            state = VoidWillBeInIsolation,
            country = ENGLAND,
            days = 8,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_isolation_book_test,
            isolationRequestInfoStringResource = R.string.state_test_void_info,
            isolationRequestInfoColorResource = R.color.amber,
            title1 = R.string.test_result_void_continue_self_isolate_title_1,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            furtherAdviceStringResource = R.string.test_result_void_continue_self_isolate_advice,
            onlineServiceLinkText = R.string.test_result_void_continue_self_isolate_nhs_guidance_label,
            onlineServiceLinkUrl = R.string.url_nhs_guidance,
            paragraphResources = intArrayOf(R.string.test_result_void_continue_self_isolate_explanation)
        )
    }

    @Test
    fun showVoidWillBeInIsolation_wales() {
        checkIsolationState(
            state = VoidWillBeInIsolation,
            country = WALES,
            days = 8,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_isolation_book_test,
            isolationRequestInfoStringResource = R.string.state_test_void_info_wls,
            isolationRequestInfoColorResource = R.color.amber,
            title1 = R.string.test_result_void_continue_self_isolate_title_1_wls,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            furtherAdviceStringResource = R.string.test_result_void_continue_self_isolate_advice_wls,
            onlineServiceLinkText = R.string.test_result_void_continue_self_isolate_nhs_guidance_label_wls,
            onlineServiceLinkUrl = R.string.url_nhs_guidance,
            paragraphResources = intArrayOf(R.string.test_result_void_continue_self_isolate_explanation_wls)
        )
    }

    @Test
    fun showPlodWillContinueWithCurrentStateScreen_england() {
        checkGoodNewsState(
            state = PlodWillContinueWithCurrentState,
            country = ENGLAND,
            hasCloseToolbar = true,
            iconDrawableRes = null,
            titleStringResource = R.string.test_result_plod_title,
            subtitleStringResource = R.string.test_result_plod_subtitle,
            actionButtonStringResource = R.string.back_to_home,
            paragraphResources = intArrayOf(R.string.test_result_plod_info),
            goodNewsInfoStringResource = R.string.test_result_plod_description,
            hasGoodNewsLink = false,
        )
    }

    @Test
    fun showPlodWillContinueWithCurrentStateScreen_wales() {
        checkGoodNewsState(
            state = PlodWillContinueWithCurrentState,
            country = WALES,
            hasCloseToolbar = true,
            iconDrawableRes = null,
            titleStringResource = R.string.test_result_plod_title_wls,
            subtitleStringResource = R.string.test_result_plod_subtitle_wls,
            actionButtonStringResource = R.string.back_to_home,
            paragraphResources = intArrayOf(R.string.test_result_plod_info_wls),
            goodNewsInfoStringResource = R.string.test_result_plod_description_wls,
            hasGoodNewsLink = false,
        )
    }
}
