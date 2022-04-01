package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.containsStringResourceAt
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.isAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.paragraphsContainerContainsStringResource
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.withNullOrEmptyContentDescription
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.withStateColor
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.withStateStringResource
import uk.nhs.nhsx.covid19.android.app.testhelpers.withDrawable

class TestResultRobot(
    private val context: Context
) {

    fun checkActivityDisplaysNegativeAlreadyNotInIsolation() {
        onView(withText(R.string.test_result_negative_already_not_in_isolation_subtitle))
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_no_self_isolation_description))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysNegativeWillBeInIsolation() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_negative_info))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysNegativeWontBeInIsolation() {
        onView(withText(R.string.test_result_negative_no_self_isolation_subtitle_text))
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_no_self_isolation_description))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveWillBeInIsolation(country: PostCodeDistrict) {
        if (country == PostCodeDistrict.ENGLAND) {
            onView(withText(R.string.index_case_isolation_advice_heading_title_england))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(withText(R.string.index_case_isolation_advice_information_box_description_england))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(withId(R.id.isolationRequestTitle1))
                .check(matches(isDisplayed()))
            onView(withId(R.id.isolationRequestTitle2))
                .check(matches(not(isDisplayed())))
            onView(withId(R.id.isolationRequestTitle3))
                .check(matches(not(isDisplayed())))
        } else {
            onView(withText(R.string.self_isolate_for))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(withText(R.string.state_test_positive_info))
                .check(matches(isDisplayed()))

            onView(withText(R.string.test_result_negative_then_positive_continue_explanation))
                .check(matches(isDisplayed()))
            onView(withId(R.id.isolationRequestTitle1))
                .check(matches(isDisplayed()))
            onView(withId(R.id.isolationRequestTitle2))
                .check(matches(isDisplayed()))
        }
    }

    fun checkActivityDisplaysPositiveWillBeInIsolation(remainingDaysInIsolation: Int) {
        onView(withText(R.string.self_isolate_for))
            .check(matches(isDisplayed()))

        val title2 = context.resources.getQuantityString(
            R.plurals.state_isolation_days,
            remainingDaysInIsolation,
            remainingDaysInIsolation
        )
        onView(withId(R.id.isolationRequestTitle2))
            .check(matches(withText(title2)))

        onView(withText(R.string.state_test_positive_info))
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_negative_then_positive_continue_explanation))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveContinueIsolation(country: PostCodeDistrict) {
        if (country == ENGLAND) {
            onView(withText(R.string.index_case_continue_isolation_advice_heading_title_england))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(
                allOf(
                    withId(R.id.stateTextView),
                    withText(R.string.index_case_isolation_advice_information_box_description_england)
                )
            )
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(withId(R.id.isolationRequestTitle1))
                .check(matches(isDisplayed()))
            onView(withId(R.id.isolationRequestTitle2))
                .check(matches(not(isDisplayed())))
            onView(withId(R.id.isolationRequestTitle3))
                .check(matches(not(isDisplayed())))
        } else {
            onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
                .check(matches(isDisplayed()))

            onView(withText(R.string.state_test_positive_info))
                .check(matches(isDisplayed()))

            onView(withText(R.string.test_result_positive_continue_self_isolate_explanation_1))
                .check(matches(isDisplayed()))
        }
    }

    fun checkActivityDisplaysPositiveContinueIsolation(remainingDaysInIsolation: Int) {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        val title2 = context.resources.getQuantityString(
            R.plurals.state_isolation_days,
            remainingDaysInIsolation,
            remainingDaysInIsolation
        )
        onView(withId(R.id.isolationRequestTitle2))
            .check(matches(withText(title2)))

        onView(withText(R.string.state_test_positive_info))
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_positive_continue_self_isolate_explanation_1))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveContinueIsolationNoChange() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_positive_info))
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_positive_continue_self_isolate_no_change_explanation_1))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPositiveWillBeInIsolationAndOrderTest(country: PostCodeDistrict) {
        if (country == ENGLAND) {
            onView(withText(R.string.index_case_isolation_advice_heading_title_england))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(withText(R.string.index_case_isolation_advice_information_box_description_england))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
        } else {
            onView(withText(R.string.self_isolate_for))
                .check(matches(isDisplayed()))

            onView(withText(R.string.test_result_positive_self_isolate_and_book_test_title_3))
                .check(matches(isDisplayed()))

            onView(withText(R.string.state_test_positive_and_book_test_info))
                .check(matches(isDisplayed()))
        }
    }

    fun checkActivityDisplaysPositiveWontBeInIsolation() {
        onView(withId(R.id.goodNewsSubtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_positive_no_self_isolation_subtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withText(R.string.test_result_no_self_isolation_description))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_positive_then_negative_info))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysVoidNotInIsolation() {
        onView(withText(R.string.test_result_void_already_not_in_isolation_subtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_no_self_isolation_description))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysPlodScreen() {
        onView(withText(R.string.test_result_plod_title))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_plod_subtitle))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText(R.string.test_result_plod_description))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysVoidWillBeInIsolation() {
        onView(withText(R.string.test_result_positive_continue_self_isolation_title_1))
            .check(matches(isDisplayed()))

        onView(withText(R.string.state_test_void_info))
            .check(matches(isDisplayed()))
    }

    fun clickIsolationActionButton() {
        onView(withId(R.id.isolationRequestActionButton)).perform(scrollTo(), click())
    }

    fun clickGoodNewsActionButton() {
        onView(withId(R.id.goodNewsActionButton)).perform(scrollTo(), click())
    }

    fun clickCloseButton() {
        onView(withContentDescription(R.string.close)).perform(click())
    }

    fun clickServiceLink() {
        onView(withId(R.id.isolationRequestOnlineServiceLink))
            .perform(scrollTo(), click())
    }

    fun checkGoodNewsActionButtonShowsOrderFreeTest() {
        onView(withId(R.id.goodNewsActionButton))
            .check(matches(withText(R.string.book_free_test)))
    }

    fun checkIsolationActionButtonShowsBookFreeTest() {
        onView(withId(R.id.isolationRequestActionButton))
            .check(matches(withText(R.string.book_free_test)))
    }

    fun checkGoodNewsActionButtonShowsContinue() {
        onView(withId(R.id.goodNewsActionButton))
            .check(matches(withText(R.string.continue_button)))
    }

    fun checkGoodNewsActionButtonShowsBackHome() {
        onView(withId(R.id.goodNewsActionButton))
            .check(matches(withText(R.string.back_to_home)))
    }

    fun checkIsolationActionButtonShowsContinue() {
        onView(withId(R.id.isolationRequestActionButton))
            .check(matches(withText(R.string.continue_button)))
    }

    fun checkIsolationActionButtonShowsAnonymouslyNotifyOthers() {
        onView(withId(R.id.isolationRequestActionButton))
            .check(matches(withText(R.string.index_case_isolation_advice_primary_button_title_england)))
    }

    fun checkIsolationActionButtonShowsBackHome() {
        onView(withId(R.id.isolationRequestActionButton))
            .check(matches(withText(R.string.back_to_home)))
    }

    fun checkExposureLinkIsDisplayed() {
        onView(withId(R.id.exposureFaqsLink))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkExposureLinkIsNotDisplayed() {
        onView(withId(R.id.isolationRequestActionButton))
            .perform(scrollTo())

        onView(withId(R.id.exposureFaqsLink))
            .check(matches(not(isDisplayed())))
    }

    private fun ViewInteraction.checkDisplayed(isVisible: Boolean) {
        if (isVisible) check(matches(isDisplayed()))
        else check(matches(not(isDisplayed())))
    }

    private fun ViewInteraction.hasOrderedParagraphs(@StringRes vararg stringResourceIdList: Int) {
        stringResourceIdList.forEachIndexed { index, stringResourceId ->
            this.check(matches(containsStringResourceAt(stringResourceId, index)))
        }
    }

    fun checkGoodNewsVisibility(isVisible: Boolean) {
        onView(withId(R.id.goodNewsContainer))
            .checkDisplayed(isVisible)
    }

    fun checkIsolationRequestVisibility(isVisible: Boolean) {
        onView(withId(R.id.isolationRequestContainer))
            .checkDisplayed(isVisible)
    }

    fun checkGoodNewsIconVisibility(isVisible: Boolean) {
        onView(withId(R.id.goodNewsIcon)).apply {
            if (isVisible) {
                perform(scrollTo())
                check(matches(isDisplayed()))
            } else check(matches(not(isDisplayed())))
        }
    }

    fun checkGoodNewsLinkVisibility(isVisible: Boolean) {
        onView(withId(R.id.goodNewsOnlineServiceLink)).apply {
            if (isVisible) {
                perform(scrollTo())
                check(matches(isDisplayed()))
            } else check(matches(not(isDisplayed())))
        }
    }

    fun checkGoodNewsIcon(drawableResourceId: Int?) {
        if (drawableResourceId != null) {
            onView(withId(R.id.goodNewsIcon))
                .check(matches(withDrawable(drawableResourceId)))
        }
    }

    fun checkGoodNewsTitleIsVisible() {
        onView(withId(R.id.goodNewsTitle))
            .checkDisplayed(true)
    }

    fun checkGoodNewsTitleStringResource(stringResourceId: Int) {
        onView(withId(R.id.goodNewsTitle))
            .check(matches(withText(stringResourceId)))
    }

    fun checkGoodNewsSubtitleStringResource(stringResourceId: Int) {
        onView(withId(R.id.goodNewsSubtitle))
            .check(matches(withText(stringResourceId)))
    }

    fun checkGoodNewsInfoState(stringResourceId: Int) {
        onView(withId(R.id.goodNewsInfoView)).apply {
            check(matches(withStateStringResource(stringResourceId)))
            check(matches(withStateColor(R.color.amber)))
        }
    }

    fun checkIsolationRequestInfoState(stringResourceId: Int, colourRes: Int) {
        onView(withId(R.id.isolationRequestInfoView)).apply {
            check(matches(withStateStringResource(stringResourceId)))
            check(matches(withStateColor(colourRes)))
        }
    }

    fun checkGoodNewsParagraphContainStringResources(vararg stringResourceIdList: Int) {
        with(onView(withId(R.id.goodNewsParagraphContainer))) {
            if (stringResourceIdList.size == 1)
                check(matches(paragraphsContainerContainsStringResource(stringResourceIdList[0])))
            else
                hasOrderedParagraphs(*stringResourceIdList)
        }
    }

    fun checkGoodNewsActionButtonTextStringResource(stringResourceId: Int) {
        onView(withId(R.id.goodNewsActionButton))
            .check(matches(withText(stringResourceId)))
    }

    fun checkIsolationRequestActionButton(stringResourceId: Int) {
        if (stringResourceId < 0) return
        onView(withId(R.id.isolationRequestActionButton))
            .check(matches(withText(stringResourceId)))
    }

    fun checkIsolationRequestImageIs(drawableResourceId: Int) {
        onView(withId(R.id.isolationRequestImage))
            .check(matches(withDrawable(drawableResourceId)))
    }

    fun checkIsolationRequestTitle1Is(stringResourceId: Int) {
        onView(withId(R.id.isolationRequestTitle1))
            .check(matches(withText(stringResourceId)))
    }

    fun checkIsolationRequestTitle2Is(text: String) {
        onView(withId(R.id.isolationRequestTitle2))
            .check(matches(withText(text)))
    }

    fun checkIsolationRequestTitle3Visibility(isVisible: Boolean) {
        onView(withId(R.id.isolationRequestTitle3))
            .checkDisplayed(isVisible)
    }

    fun checkAccessibilityContainerContentDescription(text: String) {
        onView(withId(R.id.accessibilityContainer))
            .check(matches(withContentDescription(text)))
    }

    fun checkAccessibilityContainerIsAccessibilityHeading() {
        onView(withId(R.id.accessibilityContainer))
            .check(matches(isAccessibilityHeading()))
    }

    fun checkIsolationRequestTitle1IsNotAccessibilityHeading() {
        onView(withId(R.id.isolationRequestTitle1))
            .check(matches(not(isAccessibilityHeading())))
    }

    fun checkIsolationRequestTitle2IsNotAccessibilityHeading() {
        onView(withId(R.id.isolationRequestTitle1))
            .check(matches(not(isAccessibilityHeading())))
    }

    fun checkIsolationRequestTitle3IsNotAccessibilityHeading() {
        onView(withId(R.id.isolationRequestTitle1))
            .check(matches(not(isAccessibilityHeading())))
    }

    fun checkIsolationRequestTitle1HasNullContentDescription() {
        onView(withId(R.id.isolationRequestTitle1))
            .check(matches(withNullOrEmptyContentDescription()))
    }

    fun checkIsolationRequestTitle2HasNullContentDescription() {
        onView(withId(R.id.isolationRequestTitle2))
            .check(matches(withNullOrEmptyContentDescription()))
    }

    fun checkIsolationRequestTitle3HasNullContentDescription() {
        onView(withId(R.id.isolationRequestTitle3))
            .check(matches(withNullOrEmptyContentDescription()))
    }

    fun checkExposureFaqsLinkNotVisible() {
        onView(withId(R.id.exposureFaqsLink))
            .checkDisplayed(false)
    }

    fun checkExposureFaqsLinkVisible() {
        onView(withId(R.id.exposureFaqsLink)).apply {
            perform(scrollTo())
            checkDisplayed(true)
        }
    }

    fun checkOnlineServiceLinkText(@StringRes text: Int) {
        onView(withId(R.id.isolationRequestOnlineServiceLink))
            .check(matches(withText(text)))
    }

    fun checkIsolationRequestParagraphContainerContains(vararg stringResourceIdList: Int) {
        onView(withId(R.id.isolationRequestParagraphContainer))
            .hasOrderedParagraphs(*stringResourceIdList)
    }

    fun checkHasCloseToolbarOption() {
        onView(
            allOf(
                Matchers.instanceOf(AppCompatImageButton::class.java),
                withParent(withId(R.id.primaryToolbar))
            )
        ).check(matches(withContentDescription(R.string.close)))
    }

    fun checkNoCloseToolbarOption() {
        onView(withId(R.id.primaryToolbar))
            .check(matches(hasChildCount(0)))
    }
}
