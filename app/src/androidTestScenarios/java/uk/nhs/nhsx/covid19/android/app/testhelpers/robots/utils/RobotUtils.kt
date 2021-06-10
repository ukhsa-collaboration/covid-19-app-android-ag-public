package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation.CENTER
import androidx.test.espresso.action.GeneralLocation.TOP_CENTER
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press.FINGER
import androidx.test.espresso.action.Swipe.SLOW
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

fun clickDialogPositiveButton() {
    onView(withId(android.R.id.button1))
        .inRoot(RootMatchers.isDialog())
        .perform(click())
}

fun clickDialogNegativeButton() {
    onView(withId(android.R.id.button2))
        .inRoot(RootMatchers.isDialog())
        .perform(click())
}

fun checkActivityTitleIsDisplayed(@StringRes title: Int) {
    onView(
        Matchers.allOf(
            Matchers.instanceOf(AppCompatTextView::class.java),
            ViewMatchers.withParent(withId(R.id.toolbar))
        )
    ).check(ViewAssertions.matches(ViewMatchers.withText(title)))
}

fun datePickerSelectDayOfMonth(dayOfMonth: Int) {
    try {
        datePickerSelectDay(dayOfMonth)
    } catch (e: Exception) {
        onView(withId(R.id.month_navigation_previous)).perform(click())
        waitFor { datePickerSelectDay(dayOfMonth) }
    }
    onView(withId(R.id.confirm_button)).perform(click())
}

private fun datePickerSelectDay(dayOfMonth: Int) {
    onView(withId(R.id.mtrl_calendar_months))
        .perform(swipeUpFromCenter())
    onView(
        CoreMatchers.allOf(
            ViewMatchers.isDescendantOfA(withId(R.id.mtrl_calendar_months)),
            isCompletelyDisplayed(),
            ViewMatchers.withText(dayOfMonth.toString())
        )
    )
        .perform(click())
}

private fun swipeUpFromCenter() = GeneralSwipeAction(SLOW, CENTER, TOP_CENTER, FINGER)
