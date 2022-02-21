package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
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

    private fun getString(resourceId: Int): String =
        context.resources.getString(resourceId)

    @After
    fun resetViewModel() {
        MockTestResultViewModel.currentOptions =
            MockTestResultViewModel.currentOptions.copy(
                useMock = false
            )
    }

    private fun setState(
        state: TestResultViewState,
        acknowledgementCompletionActions: AcknowledgementCompletionActions,
        days: Int = -1
    ) {
        MockTestResultViewModel.currentOptions = MockTestResultViewModel.currentOptions.copy(
            useMock = true,
            viewState = state,
            actions = acknowledgementCompletionActions,
            remainingDaysInIsolation = days
        )

        startTestActivity<TestResultActivity>()
    }

    private fun checkGoodNewsState(
        state: TestResultViewState,
        hasCloseToolbar: Boolean,
        @DrawableRes iconDrawableRes: Int?,
        @StringRes titleStringResource: Int = -1,
        @StringRes subtitleStringResource: Int,
        @StringRes actionButtonStringResource: Int,
        @StringRes vararg paragraphResources: Int,
        @StringRes goodNewsInfoStringResource: Int,
        hasGoodNewsLink: Boolean
    ) {
        setState(
            state, AcknowledgementCompletionActions(
                suggestBookTest = NoTest,
                shouldAllowKeySubmission = false
            )
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
        }

        setScreenOrientation(LANDSCAPE)
        testResultRobot.checkGoodNewsIconVisibility(false)
        setScreenOrientation(PORTRAIT)
    }

    private fun checkIsolationState(
        state: TestResultViewState,
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
        @StringRes onlineServiceLinkText: Int,
        @StringRes onlineServiceLinkUrl: Int,
        @StringRes vararg paragraphResources: Int,
        acknowledgementCompletionActions: AcknowledgementCompletionActions = AcknowledgementCompletionActions(
            suggestBookTest = NoTest,
            shouldAllowKeySubmission = false
        )
    ) {
        setState(state, acknowledgementCompletionActions, days)

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
    fun showNegativeNotInIsolation() {
        checkGoodNewsState(
            state = NegativeNotInIsolation,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.expiration_notification_title,
            subtitleStringResource = R.string.test_result_negative_already_not_in_isolation_subtitle,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.for_further_advice_visit),
            goodNewsInfoStringResource = R.string.test_result_no_self_isolation_description,
            hasGoodNewsLink = true
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
            goodNewsInfoStringResource = R.string.test_result_no_self_isolation_description,
            hasGoodNewsLink = true
        )
    }

    @Test
    fun showPositiveContinueIsolation() {
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
            exposureLinksVisible = true,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(
                R.string.test_result_positive_continue_self_isolate_explanation_1,
                R.string.test_result_positive_continue_self_isolate_explanation_2,
                R.string.exposure_faqs_title
            )
        )
    }

    @Test
    fun showPositiveContinueIsolationNoChange() {
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
    fun showPositiveWillBeInIsolation() {
        checkIsolationState(
            state = PositiveWillBeInIsolation,
            days = 6,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_positive_info,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.self_isolate_for,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = true,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(
                R.string.test_result_negative_then_positive_continue_explanation,
                R.string.exposure_faqs_title
            )
        )
    }

    @Test
    fun showPositiveWontBeInIsolation() {
        checkGoodNewsState(
            state = PositiveWontBeInIsolation,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.test_result_your_test_result,
            subtitleStringResource = R.string.test_result_positive_no_self_isolation_subtitle,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.for_further_advice_visit),
            goodNewsInfoStringResource = R.string.test_result_no_self_isolation_description,
            hasGoodNewsLink = true
        )
    }

    @Test
    fun showNegativeAfterPositiveOrSymptomaticWillBeInIsolation() {
        checkIsolationState(
            state = NegativeAfterPositiveOrSymptomaticWillBeInIsolation,
            days = 7,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_continue,
            isolationRequestInfoStringResource = R.string.state_test_positive_then_negative_info,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.test_result_positive_continue_self_isolation_title_1,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            onlineServiceLinkText = R.string.nhs_111_online_service,
            onlineServiceLinkUrl = R.string.url_nhs_111_online,
            paragraphResources = intArrayOf(R.string.test_result_positive_then_negative_explanation)
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
    fun showVoidNotInIsolation() {
        checkGoodNewsState(
            state = VoidNotInIsolation,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_elbow_bump,
            titleStringResource = R.string.test_result_your_test_result,
            subtitleStringResource = R.string.test_result_void_already_not_in_isolation_subtitle,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.for_further_advice_visit),
            goodNewsInfoStringResource = R.string.test_result_no_self_isolation_description,
            hasGoodNewsLink = true
        )
    }

    @Test
    fun showVoidWillBeInIsolation() {
        checkIsolationState(
            state = VoidWillBeInIsolation,
            days = 8,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_isolation_book_test,
            isolationRequestInfoStringResource = R.string.state_test_void_info,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.test_result_positive_continue_self_isolation_title_1,
            title3Visible = false,
            actionButtonStringResource = R.string.continue_button,
            exposureLinksVisible = false,
            onlineServiceLinkText = R.string.test_result_void_continue_self_isolate_nhs_guidance_label,
            onlineServiceLinkUrl = R.string.url_nhs_guidance,
            paragraphResources = intArrayOf(R.string.test_result_void_continue_self_isolate_explanation)
        )
    }

    @Test
    fun showPlodWillContinueWithCurrentStateScreen() {
        checkGoodNewsState(
            state = PlodWillContinueWithCurrentState,
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
}
