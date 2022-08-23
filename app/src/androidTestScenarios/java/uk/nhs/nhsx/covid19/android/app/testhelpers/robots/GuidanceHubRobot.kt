package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class GuidanceHubRobot : HasActivity {
    override val containerId: Int
        get() = id.guidanceHubContainer

    fun clickItemOne() {
        onView(withId(id.itemOne)).perform(scrollTo(), click())
    }

    fun clickItemTwo() {
        onView(withId(id.itemTwo)).perform(scrollTo(), click())
    }

    fun clickItemThree() {
        onView(withId(id.itemThree)).perform(scrollTo(), click())
    }

    fun clickItemFour() {
        onView(withId(id.itemFour)).perform(scrollTo(), click())
    }

    fun clickItemFive() {
        onView(withId(id.itemFive)).perform(scrollTo(), click())
    }

    fun clickItemSix() {
        onView(withId(id.itemSix)).perform(scrollTo(), click())
    }

    fun clickItemSeven() {
        onView(withId(id.itemSeven)).perform(scrollTo(), click())
    }

    fun clickItemEight() {
        onView(withId(id.itemEight)).perform(scrollTo(), click())
    }

    fun checkNewLabelIsDisplayed(isDisplayed: Boolean) {
        onView(withId(id.itemSeven)).perform(scrollTo())
        onView(allOf(withId(id.navigationItemNewLabel), isDescendantOfA(withId(id.itemSeven))))
            .check(ViewAssertions.matches(if (isDisplayed) ViewMatchers.isDisplayed() else not(ViewMatchers.isDisplayed())))
    }
}
