package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class WhenNotToPauseContactTracingRobot {
    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.when_not_to_pause_contact_tracing_title)
    }

    fun pressBackArrow() {
        onView(withContentDescription(R.string.go_back))
            .perform(click())
    }

    private fun checkActivityTitleIsDisplayed(@StringRes title: Int) {
        waitFor {
            onView(
                Matchers.allOf(
                    Matchers.instanceOf(AppCompatTextView::class.java),
                    ViewMatchers.withParent(ViewMatchers.withId(R.id.primaryToolbar))
                )
            ).check(ViewAssertions.matches(ViewMatchers.withText(title)))
        }
    }
}
