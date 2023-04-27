package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.TextViewDrawableMatcher
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.isAnnouncedAsOpenInBrowser
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class DecommissioningClosureScreenRobot : HasActivity {
    override val containerId: Int
        get() = id.decommissioningClosureScreenContainer

    fun clickExternalUrlSectionLink_opensInExternalBrowser(urlTitle: String) {
        onView(withText(urlTitle)).check(
            ViewAssertions.matches(
                Matchers.allOf(
                    TextViewDrawableMatcher.withTextViewHasDrawableEnd(),
                    isAnnouncedAsOpenInBrowser()
                )
            )
        )
        onView(withText(urlTitle))
            .perform(scrollTo())
            .perform(click())
    }
}
