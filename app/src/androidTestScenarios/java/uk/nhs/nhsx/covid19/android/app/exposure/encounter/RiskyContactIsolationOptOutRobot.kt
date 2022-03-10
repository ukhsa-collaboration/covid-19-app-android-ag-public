package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.TextViewDrawableMatcher.Companion.withTextViewHasDrawableEnd
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.isAnnouncedAsOpenInBrowser
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class RiskyContactIsolationOptOutRobot : HasActivity {

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
            .perform(click()) }

    fun clickGuidance_opensInExternalBrowser() {
        onView(withId(id.nhsGuidanceLinkTextView)).check(
            matches(allOf(withTextViewHasDrawableEnd(), isAnnouncedAsOpenInBrowser()))
        )
        onView(withId(id.nhsGuidanceLinkTextView))
            .perform(scrollTo())
            .perform(click()) }
}
