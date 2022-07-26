package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.IconTextViewMatcher
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.TextViewDrawableMatcher.Companion.withTextViewHasDrawableEnd
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.isAnnouncedAsOpenInBrowser
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class RiskyContactIsolationOptOutRobot : HasActivity {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    override val containerId = id.riskyContactIsolationOptOutContainer

    fun clickSecondaryButton() {
        onView(withId(id.secondaryActionButton))
            .perform(scrollTo())
            .perform(click())
    }

    fun clickPrimaryButton_opensInExternalBrowser() {
        onView(withId(id.primaryActionButton)).check(
            matches(allOf(withTextViewHasDrawableEnd(), isAnnouncedAsOpenInBrowser()))
        )
        onView(withId(id.primaryActionButton))
            .perform(scrollTo())
            .perform(click())
    }

    fun checkPrimaryButtonUrl(country: SupportedCountry) {
        val furtherAdviceLink = if (country == ENGLAND) {
            context.getString(string.risky_contact_opt_out_primary_button_url)
        } else {
            context.getString(string.risky_contact_opt_out_primary_button_url_wales)
        }
        assertBrowserIsOpened(furtherAdviceLink) {
            clickPrimaryButton_opensInExternalBrowser()
        }
    }

    fun checkWalesAdviceCopiesAreDisplayed() {
        onView(withText(string.risky_contact_opt_out_advice_title_wales))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        checkAdvice(
            viewId = id.riskyContactAdviceFreshAir,
            text = context.getString(string.risky_contact_opt_out_advice_meeting_indoors_wales),
            drawableRes = R.drawable.ic_meeting_outdoor
        )
        checkAdvice(
            viewId = id.riskyContactAdviceFaceCovering,
            text = context.getString(string.risky_contact_opt_out_advice_mask_wales),
            drawableRes = R.drawable.ic_mask
        )
        checkAdvice(
            viewId = id.riskyContactAdviceTestingHub,
            text = context.getString(string.risky_contact_opt_out_advice_testing_hub_wales),
            drawableRes = R.drawable.ic_policy_default
        )
        checkAdvice(
            viewId = id.riskyContactAdviceWashHands,
            text = context.getString(string.risky_contact_opt_out_advice_wash_hands_wales),
            drawableRes = R.drawable.ic_wash_hands
        )
        onView(withText(string.risky_contact_opt_out_primary_button_title_wales))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText(string.risky_contact_opt_out_secondary_button_title_wales))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkEnglandAdviceCopiesAreDisplayed() {
        onView(withText(string.risky_contact_opt_out_advice_title))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        checkAdvice(
            viewId = id.riskyContactAdviceFreshAir,
            text = context.getString(string.risky_contact_opt_out_advice_meeting_indoors),
            drawableRes = R.drawable.ic_meeting_outdoor
        )
        checkAdvice(
            viewId = id.riskyContactAdviceFaceCovering,
            text = context.getString(string.risky_contact_opt_out_advice_mask),
            drawableRes = R.drawable.ic_mask
        )
        checkAdvice(
            viewId = id.riskyContactAdviceWashHands,
            text = context.getString(string.risky_contact_opt_out_advice_wash_hands),
            drawableRes = R.drawable.ic_wash_hands
        )
        onView(withId(id.riskyContactAdviceTestingHub))
            .check(matches(not(isDisplayed())))
        onView(withText(string.risky_contact_opt_out_primary_button_title))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText(string.risky_contact_opt_out_secondary_button_title))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
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
}
