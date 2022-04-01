package uk.nhs.nhsx.covid19.android.app.questionnaire

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.IconTextViewMatcher
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.TextViewDrawableMatcher.Companion.withTextViewHasDrawableEnd
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.isAnnouncedAsOpenInBrowser
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class NewGuidanceForSymptomaticCasesEnglandRobot : HasActivity {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    override val containerId: Int = id.symptomaticContactIsolationGuidanceContainer

    fun checkGuidanceIsDisplayed() {
        onView(withText(string.symptomatic_contact_guidance_title_england))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        checkAdvice(
            viewId = id.symptomaticContactAdviceFaceCovering,
            text = context.getString(string.symptomatic_contact_guidance_mask_england),
            drawableRes = R.drawable.ic_mask
        )

        checkAdvice(
            viewId = id.symptomaticContactAdviceTestingHub,
            text = context.getString(string.symptomatic_contact_guidance_testing_hub_england),
            drawableRes = R.drawable.ic_social_distancing
        )

        checkAdvice(
            viewId = id.symptomaticContactAdviceFreshAir,
            text = context.getString(string.symptomatic_contact_guidance_meeting_indoors_england),
            drawableRes = R.drawable.ic_meeting_outdoor
        )

        checkAdvice(
            viewId = id.symptomaticContactAdviceWashHands,
            text = context.getString(string.symptomatic_contact_guidance_wash_hands_england),
            drawableRes = R.drawable.ic_wash_hands
        )

        onView(withId(id.exposureFaqsLinkTextView))
            .perform(scrollTo())
            .check(
                matches(
                    allOf(
                        withText(string.symptomatic_contact_guidance_common_questions_link_england),
                        isDisplayed()
                    )
                )
            )
        onView(withId(R.id.forFurtherAdviseTextView))
            .perform(scrollTo())
            .check(
                matches(
                    allOf(
                        withText(string.symptomatic_contact_guidance_further_advice_england),
                        isDisplayed()
                    )
                )
            )
        onView(withId(id.onlineServiceLinkTextView))
            .perform(scrollTo())
            .check(
                matches(
                    allOf(
                        withText(string.symptomatic_contact_guidance_nhs_online_link_england),
                        isDisplayed()
                    )
                )
            )
    }

    private fun checkAdvice(@IdRes viewId: Int, text: String, @DrawableRes drawableRes: Int) {
        onView(withId(viewId))
            .perform(scrollTo())
            .check(
                matches(
                    IconTextViewMatcher.withIconAndText(
                        text,
                        drawableRes
                    )
                )
            )
    }

    fun checkCommonQuestionsUrl() {
        assertBrowserIsOpened(context.getString(string.url_exposure_faqs)) {
            clickCommonQuestions_opensInExternalBrowser()
        }
    }

    fun checkNHSOnlineServiceUrl() {
        assertBrowserIsOpened(context.getString(string.url_nhs_111_online)) {
            clickNHSOnlineService_opensInExternalBrowser()
        }
    }

    private fun clickCommonQuestions_opensInExternalBrowser() {
        onView(withId(id.exposureFaqsLinkTextView)).check(
            matches(
                allOf(
                    withTextViewHasDrawableEnd(),
                    withText(context.getString(string.symptomatic_contact_guidance_common_questions_link_england)),
                    isAnnouncedAsOpenInBrowser()
                )
            )
        )
        onView(withId(id.exposureFaqsLinkTextView))
            .perform(scrollTo())
            .perform(click())
    }

    private fun clickNHSOnlineService_opensInExternalBrowser() {
        onView(withId(id.onlineServiceLinkTextView)).check(
            matches(
                allOf(
                    withTextViewHasDrawableEnd(),
                    withText(context.getString(string.symptomatic_contact_guidance_nhs_online_link_england)),
                    isAnnouncedAsOpenInBrowser()
                )
            )
        )
        onView(withId(id.onlineServiceLinkTextView))
            .perform(scrollTo())
            .perform(click())
    }

    fun clickPrimaryActionButton() {
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .perform(click())
    }
}
