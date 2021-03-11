package uk.nhs.nhsx.covid19.android.app.testordering

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.di.viewmodel.MockTestResultViewModel
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeAfterPositiveOrSymptomaticWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.NegativeWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveContinueIsolationNoChange
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidNotInIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultViewState.VoidWillBeInIsolation

class TestResultActivityStateTest : EspressoTest() {
    val context = testAppContext.app
    private val testResultRobot = TestResultRobot(context)

    private fun getString(resourceId: Int): String =
        context.resources.getString(resourceId)

    @After
    fun resetViewModel() {
        MockTestResultViewModel.currentOptions =
            MockTestResultViewModel.currentOptions.copy(
                useMock = false
            )
    }

    private fun setState(state: TestResultViewState, days: Int = -1) {
        MockTestResultViewModel.currentOptions = MockTestResultViewModel.currentOptions.copy(
            useMock = true,
            viewState = state,
            remainingDaysInIsolation = days
        )

        startTestActivity<TestResultActivity>()
    }

    private fun checkGoodNewsState(
        state: TestResultViewState,
        hasCloseToolbar: Boolean,
        iconDrawableRes: Int,
        titleStringResource: Int = -1,
        subtitleStringResource: Int,
        actionButtonStringResource: Int,
        vararg paragraphResources: Int
    ) {
        setState(state)

        testResultRobot.apply {
            if (hasCloseToolbar) checkHasCloseToolbarOption() else checkNoCloseToolbarOption()
            checkGoodNewsVisibility(true)
            checkIsolationRequestVisibility(false)

            checkGoodNewsIcon(iconDrawableRes)
            checkGoodNewsIconVisibility(true)
            checkGoodNewsTitleIsVisible()
            if (titleStringResource > 0) checkGoodNewsTitleStringResource(titleStringResource)
            checkGoodNewsSubtitleStringResource(subtitleStringResource)
            checkGoodNewsInfoState()
            checkGoodNewsParagraphContainStringResources(*paragraphResources)

            checkGoodNewsActionButtonTextStringResource(actionButtonStringResource)
            checkExposureFaqsLinkNotVisible()
        }

        with(UiDevice.getInstance(getInstrumentation())) {
            setOrientationLeft()
            testResultRobot.checkGoodNewsIconVisibility(false)
            setOrientationNatural()
        }
    }

    private fun checkIsolationState(
        state: TestResultViewState,
        days: Int,
        hasCloseToolbar: Boolean,
        iconDrawableRes: Int,
        isolationRequestInfoStringResource: Int,
        isolationRequestInfoColorResource: Int,
        title1: Int,
        title3Visible: Boolean,
        title3: Int = -1,
        actionButtonStringResource: Int,
        exposureLinksVisible: Boolean,
        vararg paragraphResources: Int
    ) {
        setState(state, days)

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
            checkIsolationRequestActionButton(actionButtonStringResource)
        }
    }

    @Test
    fun showNegativeNotInIsolation() = notReported {
        checkGoodNewsState(
            state = NegativeNotInIsolation,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_expired_or_over,
            titleStringResource = R.string.expiration_notification_title,
            subtitleStringResource = R.string.test_result_negative_already_not_in_isolation_subtitle,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.for_further_advice_visit)
        )
    }

    @Test
    fun showNegativeWillBeInIsolation() = notReported {
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
            exposureLinksVisible = false
        )
    }

    @Test
    fun showNegativeWontBeInIsolation() = notReported {
        checkGoodNewsState(
            state = NegativeWontBeInIsolation,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_negative_or_finished,
            titleStringResource = R.string.expiration_notification_title,
            subtitleStringResource = R.string.test_result_negative_no_self_isolation_subtitle_text,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.for_further_advice_visit)
        )
    }

    @Test
    fun showPositiveContinueIsolation() = notReported {
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
            paragraphResources = intArrayOf(
                R.string.test_result_positive_continue_self_isolate_explanation_1,
                R.string.test_result_positive_continue_self_isolate_explanation_2,
                R.string.exposure_faqs_title
            )
        )
    }

    @Test
    fun showPositiveContinueIsolationNoChange() = notReported {
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
            paragraphResources = intArrayOf(R.string.test_result_positive_continue_self_isolate_no_change_explanation_1)
        )
    }

    @Test
    fun showPositiveWillBeInIsolation() = notReported {
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
            paragraphResources = intArrayOf(
                R.string.test_result_negative_then_positive_continue_explanation,
                R.string.exposure_faqs_title
            )
        )
    }

    @Test
    fun showPositiveWontBeInIsolation() = notReported {
        checkGoodNewsState(
            state = PositiveWontBeInIsolation,
            hasCloseToolbar = false,
            iconDrawableRes = R.drawable.ic_isolation_expired_or_over,
            titleStringResource = R.string.test_result_your_test_result,
            subtitleStringResource = R.string.test_result_positive_no_self_isolation_subtitle,
            actionButtonStringResource = R.string.continue_button,
            paragraphResources = intArrayOf(R.string.for_further_advice_visit)
        )
    }

    @Test
    fun showNegativeAfterPositiveOrSymptomaticWillBeInIsolation() = notReported {
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
            paragraphResources = intArrayOf(R.string.test_result_positive_then_negative_explanation)
        )
    }

    @Test
    fun showPositiveWillBeInIsolationAndOrderTest() = notReported {
        checkIsolationState(
            state = PositiveWillBeInIsolationAndOrderTest,
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
            paragraphResources = intArrayOf(R.string.test_result_positive_self_isolate_and_book_test_explanation_1)
        )
    }

    @Test
    fun showVoidNotInIsolation() = notReported {
        checkGoodNewsState(
            state = VoidNotInIsolation,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_isolation_expired_or_over,
            titleStringResource = R.string.test_result_your_test_result,
            subtitleStringResource = R.string.test_result_void_already_not_in_isolation_subtitle,
            actionButtonStringResource = R.string.book_free_test,
            paragraphResources = intArrayOf(R.string.for_further_advice_visit)
        )
    }

    @Test
    fun showVoidWillBeInIsolation() = notReported {
        checkIsolationState(
            state = VoidWillBeInIsolation,
            days = 8,
            hasCloseToolbar = true,
            iconDrawableRes = R.drawable.ic_isolation_book_test,
            isolationRequestInfoStringResource = R.string.state_test_void_info,
            isolationRequestInfoColorResource = R.color.error_red,
            title1 = R.string.test_result_positive_continue_self_isolation_title_1,
            title3Visible = false,
            actionButtonStringResource = R.string.book_free_test,
            exposureLinksVisible = false,
            paragraphResources = intArrayOf(R.string.test_result_void_continue_self_isolate_explanation)
        )
    }
}
