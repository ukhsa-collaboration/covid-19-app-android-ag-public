package uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

interface HasActivity {
    @get:IdRes
    val containerId: Int

    fun checkActivityIsDisplayed() = waitFor {
        onView(withId(containerId)).check(matches(isDisplayed()))
    }
}
