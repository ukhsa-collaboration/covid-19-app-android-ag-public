package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class GuidanceHubWalesRobot : HasActivity {
    override val containerId: Int
        get() = id.guidanceHubWalesContainer

    fun clickItemOne() {
        Espresso.onView(ViewMatchers.withId(id.itemOne))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemTwo() {
        Espresso.onView(ViewMatchers.withId(id.itemTwo))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemThree() {
        Espresso.onView(ViewMatchers.withId(id.itemThree)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemFour() {
        Espresso.onView(ViewMatchers.withId(id.itemFour))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemFive() {
        Espresso.onView(ViewMatchers.withId(id.itemFive))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemSix() {
        Espresso.onView(ViewMatchers.withId(id.itemSix))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemSeven() {
        Espresso.onView(ViewMatchers.withId(id.itemSeven))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }
}
