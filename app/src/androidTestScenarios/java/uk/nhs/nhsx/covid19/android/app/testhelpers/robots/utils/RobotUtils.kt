package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation.CENTER
import androidx.test.espresso.action.GeneralLocation.TOP_CENTER
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press.FINGER
import androidx.test.espresso.action.Swipe.SLOW
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import java.util.Locale

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
            isDescendantOfA(withId(R.id.mtrl_calendar_months)),
            isCompletelyDisplayed(),
            withText(dayOfMonth.toString())
        )
    )
        .perform(click())
}

private fun swipeUpFromCenter() = GeneralSwipeAction(SLOW, CENTER, TOP_CENTER, FINGER)

val context: Context // Do not cache it, since context can be changed (e.g. locale change)
    get() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val config = targetContext.resources.configuration.apply {
            setLocale(Locale.getDefault())
        }
        return targetContext.createConfigurationContext(config)
    }
