package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class GuidanceHubWalesRobot : HasActivity {
    override val containerId: Int
        get() = id.guidanceHubWalesContainer

    fun clickItemOne() {
        onView(withId(id.itemOne))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemTwo() {
        onView(withId(id.itemTwo))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemThree() {
        onView(withId(id.itemThree)).perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemFour() {
        onView(withId(id.itemFour))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemFive() {
        onView(withId(id.itemFive))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemSix() {
        onView(withId(id.itemSix))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemSeven() {
        onView(withId(id.itemSeven))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickItemEight() {
        onView(withId(id.itemEight))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }
}
